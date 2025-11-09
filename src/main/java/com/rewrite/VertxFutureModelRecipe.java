package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;

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
        return new JavaIsoVisitor<>() {

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                // Transform executeBlocking(promise -> promise.complete(value)) to executeBlocking(() -> value)
                if ("executeBlocking".equals(mi.getSimpleName()) && mi.getArguments().size() > 0) {
                    Expression firstArg = mi.getArguments().get(0);

                    if (firstArg instanceof J.Lambda lambda) {
                        mi = transformExecuteBlockingLambda(mi, lambda);
                    }
                }

                // Transform future.eventually(v -> someFuture()) to future.eventually(() -> someFuture())
                if ("eventually".equals(mi.getSimpleName()) && mi.getArguments().size() > 0) {
                    Expression firstArg = mi.getArguments().get(0);

                    if (firstArg instanceof J.Lambda lambda) {
                        mi = transformEventuallyLambda(mi, lambda);
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
                    return mi;
                }

                J body = lambda.getBody();
                J.Lambda newLambda = null;

                // Case 1: Expression body - promise -> promise.complete(value)
                if (body instanceof J.MethodInvocation bodyMethod) {
                    if ("complete".equals(bodyMethod.getSimpleName()) || "fail".equals(bodyMethod.getSimpleName())) {
                        Expression extractedValue = extractValueFromPromiseMethod(bodyMethod);
                        if (extractedValue != null) {
                            // Create new lambda with empty parameters and the extracted value as body
                            newLambda = lambda.withParameters(
                                lambda.getParameters().withParameters(java.util.Collections.emptyList())
                            ).withBody(extractedValue);
                        }
                    }
                }
                // Case 2: Block body - promise -> { statements; promise.complete(value); }
                else if (body instanceof J.Block block) {
                    J.Block transformedBlock = transformPromiseBlockToCallable(block);
                    if (transformedBlock != null) {
                        newLambda = lambda.withParameters(
                            lambda.getParameters().withParameters(java.util.Collections.emptyList())
                        ).withBody(transformedBlock);
                    }
                }

                if (newLambda != null) {
                    return mi.withArguments(
                        java.util.Collections.singletonList(newLambda)
                    );
                }

                return mi;
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
                    return mi; // Already in correct form
                }

                // Check if the parameter is used in the body
                if (lambda.getParameters().getParameters().size() == 1) {
                    J.VariableDeclarations.NamedVariable param =
                        (J.VariableDeclarations.NamedVariable) lambda.getParameters().getParameters().get(0);
                    String paramName = param.getSimpleName();

                    // Simple heuristic: if parameter is not referenced in body, we can remove it
                    boolean parameterUsed = isParameterUsedInBody(lambda.getBody(), paramName);

                    if (!parameterUsed) {
                        // Remove the parameter
                        J.Lambda newLambda = lambda.withParameters(
                            lambda.getParameters().withParameters(java.util.Collections.emptyList())
                        );

                        return mi.withArguments(
                            java.util.Collections.singletonList(newLambda)
                        );
                    }
                }

                return mi;
            }

            /**
             * Extracts the value from promise.complete(value) or promise.fail(exception)
             */
            private Expression extractValueFromPromiseMethod(J.MethodInvocation method) {
                if (method.getArguments().size() == 1) {
                    return method.getArguments().get(0);
                }
                return null;
            }

            /**
             * Transforms a block body from promise-based to return-based.
             * Replaces promise.complete(value) with return value;
             * Replaces promise.fail(exception) with throw exception;
             */
            private J.Block transformPromiseBlockToCallable(J.Block block) {
                java.util.List<org.openrewrite.java.tree.Statement> statements = block.getStatements();
                if (statements.isEmpty()) {
                    return null;
                }

                java.util.List<org.openrewrite.java.tree.Statement> newStatements = new java.util.ArrayList<>();
                boolean transformed = false;

                for (org.openrewrite.java.tree.Statement stmt : statements) {
                    if (stmt instanceof J.MethodInvocation stmtMethod) {
                        // Transform promise.complete(value) to return value
                        if ("complete".equals(stmtMethod.getSimpleName()) &&
                            stmtMethod.getArguments().size() == 1) {
                            Expression value = stmtMethod.getArguments().get(0);
                            J.Return returnStmt = new J.Return(
                                stmt.getId(),
                                stmt.getPrefix(),
                                stmt.getMarkers(),
                                value
                            );
                            newStatements.add(returnStmt);
                            transformed = true;
                            continue;
                        }
                        // Transform promise.fail(exception) to throw exception
                        else if ("fail".equals(stmtMethod.getSimpleName()) &&
                                 stmtMethod.getArguments().size() == 1) {
                            Expression exception = stmtMethod.getArguments().get(0);
                            J.Throw throwStmt = new J.Throw(
                                stmt.getId(),
                                stmt.getPrefix(),
                                stmt.getMarkers(),
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

                String bodyStr = body.toString();
                return bodyStr.contains(paramName);
            }
        };
    }
}
