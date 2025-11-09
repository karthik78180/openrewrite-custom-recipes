package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Statement;

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

                // Transform executeBlocking
                if ("executeBlocking".equals(mi.getSimpleName()) && !mi.getArguments().isEmpty()) {
                    if (mi.getArguments().get(0) instanceof J.Lambda lambda) {
                        J.MethodInvocation transformed = transformExecuteBlocking(mi, lambda);
                        if (transformed != null) {
                            return transformed;
                        }
                    }
                }

                // Transform eventually
                if ("eventually".equals(mi.getSimpleName()) && !mi.getArguments().isEmpty()) {
                    if (mi.getArguments().get(0) instanceof J.Lambda lambda) {
                        J.MethodInvocation transformed = transformEventually(mi, lambda);
                        if (transformed != null) {
                            return transformed;
                        }
                    }
                }

                return mi;
            }

            /**
             * Transform executeBlocking(promise -> promise.complete(value)) to executeBlocking(() -> value)
             */
            private J.MethodInvocation transformExecuteBlocking(J.MethodInvocation mi, J.Lambda lambda) {
                // Only transform if lambda has exactly one parameter
                if (lambda.getParameters().getParameters().size() != 1) {
                    return null;
                }

                J body = lambda.getBody();

                // Case 1: Expression body - promise -> promise.complete(value)
                if (body instanceof J.MethodInvocation bodyMethod && "complete".equals(bodyMethod.getSimpleName())) {
                    if (!bodyMethod.getArguments().isEmpty()) {
                        Expression value = bodyMethod.getArguments().get(0);

                        // Build the pattern based on whether mi has a select or not
                        if (mi.getSelect() != null) {
                            return JavaTemplate.builder("#{any()}.executeBlocking(() -> #{any()})")
                                .build()
                                .apply(
                                    getCursor(),
                                    mi.getCoordinates().replace(),
                                    mi.getSelect(),
                                    value
                                );
                        } else {
                            return JavaTemplate.builder("executeBlocking(() -> #{any()})")
                                .build()
                                .apply(
                                    getCursor(),
                                    mi.getCoordinates().replace(),
                                    value
                                );
                        }
                    }
                }
                // Case 2: Block body - promise -> { statements; promise.complete(value); }
                else if (body instanceof J.Block block) {
                    String transformedBlock = buildTransformedBlock(block);
                    if (transformedBlock != null) {
                        if (mi.getSelect() != null) {
                            return JavaTemplate.builder("#{any()}.executeBlocking(() -> " + transformedBlock + ")")
                                .build()
                                .apply(
                                    getCursor(),
                                    mi.getCoordinates().replace(),
                                    mi.getSelect()
                                );
                        } else {
                            return JavaTemplate.builder("executeBlocking(() -> " + transformedBlock + ")")
                                .build()
                                .apply(
                                    getCursor(),
                                    mi.getCoordinates().replace()
                                );
                        }
                    }
                }

                return null;
            }

            /**
             * Transform eventually(v -> someFuture()) to eventually(() -> someFuture())
             */
            private J.MethodInvocation transformEventually(J.MethodInvocation mi, J.Lambda lambda) {
                // Only transform if lambda has exactly one parameter
                if (lambda.getParameters().getParameters().isEmpty()) {
                    return null; // Already transformed
                }

                if (lambda.getParameters().getParameters().size() != 1) {
                    return null;
                }

                // Get parameter name
                String paramName = extractParameterName(lambda.getParameters().getParameters().get(0));
                if (paramName == null) {
                    return null;
                }

                // Only transform if parameter is not used
                if (isParameterUsed(lambda.getBody(), paramName)) {
                    return null;
                }

                // Get body string
                String bodyStr = lambda.getBody() != null ? lambda.getBody().print(getCursor()).trim() : "";

                // Build the pattern based on whether mi has a select or not
                if (mi.getSelect() != null) {
                    return JavaTemplate.builder("#{any()}.eventually(() -> " + bodyStr + ")")
                        .build()
                        .apply(
                            getCursor(),
                            mi.getCoordinates().replace(),
                            mi.getSelect()
                        );
                } else {
                    return JavaTemplate.builder("eventually(() -> " + bodyStr + ")")
                        .build()
                        .apply(
                            getCursor(),
                            mi.getCoordinates().replace()
                        );
                }
            }

            /**
             * Build transformed block string for executeBlocking
             */
            private String buildTransformedBlock(J.Block block) {
                java.util.List<Statement> statements = block.getStatements();
                if (statements.isEmpty()) {
                    return null;
                }

                StringBuilder sb = new StringBuilder("{ ");
                boolean transformed = false;

                for (Statement stmt : statements) {
                    if (stmt instanceof J.MethodInvocation mi) {
                        if ("complete".equals(mi.getSimpleName()) && mi.getArguments().size() == 1) {
                            sb.append("return ").append(mi.getArguments().get(0).print(getCursor())).append("; ");
                            transformed = true;
                        } else if ("fail".equals(mi.getSimpleName()) && mi.getArguments().size() == 1) {
                            sb.append("throw ").append(mi.getArguments().get(0).print(getCursor())).append("; ");
                            transformed = true;
                        } else {
                            sb.append(stmt.print(getCursor())).append(" ");
                        }
                    } else {
                        sb.append(stmt.print(getCursor())).append(" ");
                    }
                }

                sb.append("}");
                return transformed ? sb.toString() : null;
            }

            /**
             * Extract parameter name from lambda parameter
             */
            private String extractParameterName(J param) {
                if (param instanceof J.VariableDeclarations vd) {
                    if (!vd.getVariables().isEmpty()) {
                        return vd.getVariables().get(0).getSimpleName();
                    }
                } else if (param instanceof J.Identifier id) {
                    return id.getSimpleName();
                }
                return null;
            }

            /**
             * Check if parameter is used in the lambda body
             */
            private boolean isParameterUsed(J body, String paramName) {
                if (body == null) {
                    return false;
                }

                final boolean[] used = {false};
                new JavaIsoVisitor<Integer>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Integer p) {
                        if (paramName.equals(identifier.getSimpleName())) {
                            used[0] = true;
                        }
                        return super.visitIdentifier(identifier, p);
                    }
                }.visit(body, 0);

                return used[0];
            }
        };
    }
}
