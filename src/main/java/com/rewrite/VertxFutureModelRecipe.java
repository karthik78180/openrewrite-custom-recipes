package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.Cursor;

import java.util.Collections;

/**
 * Transforms Vert.x code to embrace the Future model introduced in Vert.x 5.
 *
 * Changes:
 * - vertx.executeBlocking(promise -> promise.complete(result)) to vertx.executeBlocking(() -> result)
 * - future.eventually(v -> someFuture()) to future.eventually(() -> someFuture())
 *
 * This migration removes the promise-based callback pattern in favor of
 * java.util.concurrent.Callable for executeBlocking and Supplier for eventually.
 */
public class VertxFutureModelRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Embrace Vert.x 5 Future Model";
    }

    @Override
    public String getDescription() {
        return "Transforms executeBlocking to use Callable instead of Promise handler, " +
               "and eventually to use Supplier instead of Function.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, ExecutionContext ctx) {
                J.Lambda l = super.visitLambda(lambda, ctx);

                // Check if this lambda is an argument to executeBlocking or eventually
                // Use firstEnclosing to find the nearest method invocation in the cursor path
                J.MethodInvocation mi = getCursor().firstEnclosing(J.MethodInvocation.class);
                if (mi != null) {
                    if ("executeBlocking".equals(mi.getSimpleName())) {
                        return transformExecuteBlockingLambda(l);
                    } else if ("eventually".equals(mi.getSimpleName())) {
                        return transformEventuallyLambda(l);
                    }
                }

                return l;
            }

            /**
             * Transforms executeBlocking lambda from promise-based to Callable-based.
             *
             * From: promise -> promise.complete(result)
             * To:   () -> result
             *
             * Also handles block bodies:
             * From: promise -> { doWork(); promise.complete(result); }
             * To:   () -> { doWork(); return result; }
             */
            private J.Lambda transformExecuteBlockingLambda(J.Lambda lambda) {
                // Check if lambda has a single parameter (promise)
                if (lambda.getParameters().getParameters().size() != 1) {
                    return lambda;
                }

                J body = lambda.getBody();

                // Case 1: Expression body - promise -> promise.complete(value)
                if (body instanceof J.MethodInvocation bodyMethod) {
                    if ("complete".equals(bodyMethod.getSimpleName()) && !bodyMethod.getArguments().isEmpty()) {
                        // Remove parameter and update body to just the value
                        return lambda
                            .withParameters(lambda.getParameters().withParameters(Collections.emptyList()))
                            .withBody(bodyMethod.getArguments().get(0));
                    }
                }
                // Case 2: Block body - promise -> { statements; promise.complete(value); }
                else if (body instanceof J.Block block) {
                    J.Block transformedBlock = transformPromiseBlockToCallable(block);
                    if (transformedBlock != null) {
                        return lambda
                            .withParameters(lambda.getParameters().withParameters(Collections.emptyList()))
                            .withBody(transformedBlock);
                    }
                }

                return lambda;
            }

            /**
             * Transforms eventually lambda from Function to Supplier.
             *
             * From: v -> someFuture()
             * To:   () -> someFuture()
             *
             * The parameter is removed if it's not used in the body.
             */
            private J.Lambda transformEventuallyLambda(J.Lambda lambda) {
                // Check if lambda has parameters
                if (lambda.getParameters().getParameters().isEmpty()) {
                    return lambda; // Already in correct form
                }

                // Check if the parameter is used in the body
                if (lambda.getParameters().getParameters().size() == 1) {
                    String paramName = extractParameterName(lambda.getParameters().getParameters().get(0));

                    if (paramName == null) {
                        return lambda; // Can't determine parameter name
                    }

                    // Simple heuristic: if parameter is not referenced in body, we can remove it
                    boolean parameterUsed = isParameterUsedInBody(lambda.getBody(), paramName);

                    if (!parameterUsed) {
                        // Remove the parameter
                        return lambda.withParameters(
                            lambda.getParameters().withParameters(Collections.emptyList())
                        );
                    }
                }

                return lambda;
            }

            /**
             * Extract parameter name from lambda parameter which can be either
             * J.VariableDeclarations or J.Identifier
             */
            private String extractParameterName(J param) {
                if (param instanceof J.VariableDeclarations varDecls) {
                    if (!varDecls.getVariables().isEmpty()) {
                        return varDecls.getVariables().get(0).getSimpleName();
                    }
                } else if (param instanceof J.Identifier ident) {
                    return ident.getSimpleName();
                }
                return null;
            }

            /**
             * Transforms a block body from promise-based to return-based.
             * Replaces promise.complete(value) with return value;
             * Replaces promise.fail(exception) with throw exception;
             */
            private J.Block transformPromiseBlockToCallable(J.Block block) {
                java.util.List<Statement> statements = block.getStatements();
                if (statements.isEmpty()) {
                    return null;
                }

                java.util.List<Statement> newStatements = new java.util.ArrayList<>();
                boolean transformed = false;

                for (Statement stmt : statements) {
                    if (stmt instanceof J.MethodInvocation stmtMethod) {
                        if ("complete".equals(stmtMethod.getSimpleName()) &&
                            stmtMethod.getArguments().size() == 1) {
                            Expression value = stmtMethod.getArguments().get(0);

                            // Use JavaTemplate to create a proper return statement with type info
                            J.Return returnStmt = (J.Return) JavaTemplate
                                .builder("return #{any()};")
                                .build()
                                .apply(
                                    new Cursor(getCursor(), stmt),
                                    stmt.getCoordinates().replace(),
                                    value
                                );

                            newStatements.add(returnStmt);
                            transformed = true;
                            continue;
                        } else if ("fail".equals(stmtMethod.getSimpleName()) &&
                                   stmtMethod.getArguments().size() == 1) {
                            Expression exception = stmtMethod.getArguments().get(0);

                            // Use JavaTemplate to create a proper throw statement with type info
                            J.Throw throwStmt = (J.Throw) JavaTemplate
                                .builder("throw #{any()};")
                                .build()
                                .apply(
                                    new Cursor(getCursor(), stmt),
                                    stmt.getCoordinates().replace(),
                                    exception
                                );

                            newStatements.add(throwStmt);
                            transformed = true;
                            continue;
                        }
                    }
                    newStatements.add(stmt);
                }

                if (transformed) {
                    return block.withStatements(newStatements);
                }

                return null;
            }

            /**
             * Simple check if a parameter name appears in the lambda body.
             * This is a heuristic approach - a more robust solution would use
             * variable resolution from OpenRewrite's type attribution.
             */
            private boolean isParameterUsedInBody(J body, String paramName) {
                if (body == null) {
                    return false;
                }

                // Use a visitor to check if identifier is used
                final boolean[] used = {false};
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer integer) {
                        if (paramName.equals(identifier.getSimpleName())) {
                            used[0] = true;
                        }
                        return super.visitIdentifier(identifier, integer);
                    }
                }.visit(body, 0);

                return used[0];
            }
        };
    }
}
