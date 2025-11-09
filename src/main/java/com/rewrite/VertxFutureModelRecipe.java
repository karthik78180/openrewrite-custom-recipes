package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

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
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                // Transform executeBlocking(promise -> promise.complete(value)) to executeBlocking(() -> value)
                if ("executeBlocking".equals(mi.getSimpleName()) && !mi.getArguments().isEmpty()) {
                    Expression firstArg = mi.getArguments().get(0);

                    if (firstArg instanceof J.Lambda lambda) {
                        J.MethodInvocation transformed = transformExecuteBlockingLambda(mi, lambda);
                        if (transformed != null) {
                            return transformed;
                        }
                    }
                }

                // Transform future.eventually(v -> someFuture()) to future.eventually(() -> someFuture())
                if ("eventually".equals(mi.getSimpleName()) && !mi.getArguments().isEmpty()) {
                    Expression firstArg = mi.getArguments().get(0);

                    if (firstArg instanceof J.Lambda lambda) {
                        J.MethodInvocation transformed = transformEventuallyLambda(mi, lambda);
                        if (transformed != null) {
                            return transformed;
                        }
                    }
                }

                return mi;
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
            private J.MethodInvocation transformExecuteBlockingLambda(J.MethodInvocation mi, J.Lambda lambda) {
                // Check if lambda has a single parameter (promise)
                if (lambda.getParameters().getParameters().size() != 1) {
                    return null;
                }

                J body = lambda.getBody();
                J.Lambda newLambda = null;

                // Case 1: Expression body - promise -> promise.complete(value)
                if (body instanceof J.MethodInvocation bodyMethod) {
                    if ("complete".equals(bodyMethod.getSimpleName())) {
                        if (!bodyMethod.getArguments().isEmpty()) {
                            Expression value = bodyMethod.getArguments().get(0);
                            // Create new lambda with empty parameters and the extracted value as body
                            newLambda = lambda.withParameters(
                                lambda.getParameters().withParameters(Collections.emptyList())
                            ).withBody(value);
                        }
                    }
                }
                // Case 2: Block body - promise -> { statements; promise.complete(value); }
                else if (body instanceof J.Block block) {
                    J.Block transformedBlock = transformPromiseBlockToCallable(block);
                    if (transformedBlock != null) {
                        newLambda = lambda.withParameters(
                            lambda.getParameters().withParameters(Collections.emptyList())
                        ).withBody(transformedBlock);
                    }
                }

                if (newLambda != null) {
                    return mi.withArguments(Collections.singletonList(newLambda));
                }

                return null;
            }

            /**
             * Transforms eventually lambda from Function to Supplier.
             *
             * From: v -> someFuture()
             * To:   () -> someFuture()
             *
             * The parameter is removed if it's not used in the body.
             */
            private J.MethodInvocation transformEventuallyLambda(J.MethodInvocation mi, J.Lambda lambda) {
                // Check if lambda has parameters
                if (lambda.getParameters().getParameters().isEmpty()) {
                    return null; // Already in correct form
                }

                // Check if the parameter is used in the body
                if (lambda.getParameters().getParameters().size() == 1) {
                    String paramName = extractParameterName(lambda.getParameters().getParameters().get(0));

                    if (paramName == null) {
                        return null; // Can't determine parameter name
                    }

                    // Simple heuristic: if parameter is not referenced in body, we can remove it
                    boolean parameterUsed = isParameterUsedInBody(lambda.getBody(), paramName);

                    if (!parameterUsed) {
                        // Remove the parameter
                        J.Lambda newLambda = lambda.withParameters(
                            lambda.getParameters().withParameters(Collections.emptyList())
                        );

                        return mi.withArguments(Collections.singletonList(newLambda));
                    }
                }

                return null;
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
                List<Statement> statements = block.getStatements();
                if (statements.isEmpty()) {
                    return null;
                }

                List<Statement> newStatements = new ArrayList<>();
                boolean transformed = false;

                for (Statement stmt : statements) {
                    // Handle promise.complete(value) -> return value
                    if (stmt instanceof J.MethodInvocation stmtMethod) {
                        if ("complete".equals(stmtMethod.getSimpleName()) &&
                            stmtMethod.getArguments().size() == 1) {
                            Expression value = stmtMethod.getArguments().get(0);

                            // Create a return statement
                            J.Return returnStmt = new J.Return(
                                UUID.randomUUID(),
                                stmt.getPrefix(),
                                Markers.EMPTY,
                                value
                            );

                            newStatements.add(returnStmt);
                            transformed = true;
                            continue;
                        }
                        // Handle promise.fail(exception) -> throw exception
                        else if ("fail".equals(stmtMethod.getSimpleName()) &&
                                 stmtMethod.getArguments().size() == 1) {
                            Expression exception = stmtMethod.getArguments().get(0);

                            // Create a throw statement
                            J.Throw throwStmt = new J.Throw(
                                UUID.randomUUID(),
                                stmt.getPrefix(),
                                Markers.EMPTY,
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
