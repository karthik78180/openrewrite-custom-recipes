package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class VertxFutureModelRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new VertxFutureModelRecipe());
    }

    @Test
    void executeBlockingWithExpressionBodyTransformed() {
        rewriteRun(
                // Stub classes - no changes expected
                java(
                        """
                                package io.vertx.core;
                                public interface Handler<E> {
                                    void handle(E event);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Vertx {
                                    <T> Future<T> executeBlocking(Handler<Promise<T>> blockingCodeHandler);
                                    <T> Future<T> executeBlocking(java.util.concurrent.Callable<T> callable);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Future<T> {}
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Promise<T> {
                                    void complete(T result);
                                }
                                """
                ),
                // Actual test case - executeBlocking with expression body
                java(
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class BlockingService {
                                    public Future<String> getData(Vertx vertx) {
                                        return vertx.executeBlocking(promise -> promise.complete("result"));
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class BlockingService {
                                    public Future<String> getData(Vertx vertx) {
                                        return vertx.executeBlocking(() -> "result");
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void executeBlockingWithBlockBodyTransformed() {
        rewriteRun(
                // Stub classes
                java(
                        """
                                package io.vertx.core;
                                public interface Handler<E> {
                                    void handle(E event);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Vertx {
                                    <T> Future<T> executeBlocking(Handler<Promise<T>> blockingCodeHandler);
                                    <T> Future<T> executeBlocking(java.util.concurrent.Callable<T> callable);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Future<T> {}
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Promise<T> {
                                    void complete(T result);
                                }
                                """
                ),
                // Actual test case - executeBlocking with block body
                java(
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class BlockingService {
                                    public Future<Integer> compute(Vertx vertx) {
                                        return vertx.executeBlocking(promise -> {
                                            int result = 42 + 8;
                                            promise.complete(result);
                                        });
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class BlockingService {
                                    public Future<Integer> compute(Vertx vertx) {
                                        return vertx.executeBlocking(() -> {
                                            int result = 42 + 8;
                                            return result;
                                        });
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void executeBlockingWithFailTransformed() {
        rewriteRun(
                // Stub classes
                java(
                        """
                                package io.vertx.core;
                                public interface Handler<E> {
                                    void handle(E event);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Vertx {
                                    <T> Future<T> executeBlocking(Handler<Promise<T>> blockingCodeHandler);
                                    <T> Future<T> executeBlocking(java.util.concurrent.Callable<T> callable);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Future<T> {}
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Promise<T> {
                                    void complete(T result);
                                    void fail(Throwable throwable);
                                }
                                """
                ),
                // Actual test case - executeBlocking with fail
                java(
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class BlockingService {
                                    public Future<String> riskyOperation(Vertx vertx) {
                                        return vertx.executeBlocking(promise -> {
                                            if (Math.random() > 0.5) {
                                                promise.fail(new RuntimeException("Random failure"));
                                            } else {
                                                promise.complete("success");
                                            }
                                        });
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class BlockingService {
                                    public Future<String> riskyOperation(Vertx vertx) {
                                        return vertx.executeBlocking(() -> {
                                            if (Math.random() > 0.5) {
                                                throw new RuntimeException("Random failure");
                                            } else {
                                                return "success";
                                            }
                                        });
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void eventuallyWithUnusedParameterTransformed() {
        rewriteRun(
                // Stub classes
                java(
                        """
                                package java.util.function;
                                public interface Function<T, R> {
                                    R apply(T t);
                                }
                                """
                ),
                java(
                        """
                                package java.util.function;
                                public interface Supplier<T> {
                                    T get();
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Future<T> {
                                    Future<T> eventually(java.util.function.Function<T, Future<T>> mapper);
                                    Future<T> eventually(java.util.function.Supplier<Future<T>> supplier);
                                }
                                """
                ),
                // Actual test case - eventually with unused parameter
                java(
                        """
                                package com.example;

                                import io.vertx.core.Future;

                                public class FutureService {
                                    public Future<String> chainOperations(Future<String> future) {
                                        return future.eventually(v -> cleanup());
                                    }

                                    private Future<String> cleanup() {
                                        return null;
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Future;

                                public class FutureService {
                                    public Future<String> chainOperations(Future<String> future) {
                                        return future.eventually(() -> cleanup());
                                    }

                                    private Future<String> cleanup() {
                                        return null;
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void multipleTransformationsInSameFile() {
        rewriteRun(
                // Stub classes
                java(
                        """
                                package java.util.function;
                                public interface Function<T, R> {
                                    R apply(T t);
                                }
                                """
                ),
                java(
                        """
                                package java.util.function;
                                public interface Supplier<T> {
                                    T get();
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Handler<E> {
                                    void handle(E event);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Vertx {
                                    <T> Future<T> executeBlocking(Handler<Promise<T>> blockingCodeHandler);
                                    <T> Future<T> executeBlocking(java.util.concurrent.Callable<T> callable);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Future<T> {
                                    Future<T> eventually(java.util.function.Function<T, Future<T>> mapper);
                                    Future<T> eventually(java.util.function.Supplier<Future<T>> supplier);
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Promise<T> {
                                    void complete(T result);
                                }
                                """
                ),
                // Actual test case - multiple transformations
                java(
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class ComplexService {
                                    public Future<String> processData(Vertx vertx) {
                                        Future<String> step1 = vertx.executeBlocking(promise -> promise.complete("data"));
                                        return step1.eventually(v -> cleanup());
                                    }

                                    public Future<Integer> computeValue(Vertx vertx) {
                                        return vertx.executeBlocking(promise -> {
                                            int value = 100;
                                            promise.complete(value);
                                        });
                                    }

                                    private Future<String> cleanup() {
                                        return null;
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.Future;

                                public class ComplexService {
                                    public Future<String> processData(Vertx vertx) {
                                        Future<String> step1 = vertx.executeBlocking(() -> "data");
                                        return step1.eventually(() -> cleanup());
                                    }

                                    public Future<Integer> computeValue(Vertx vertx) {
                                        return vertx.executeBlocking(() -> {
                                            int value = 100;
                                            return value;
                                        });
                                    }

                                    private Future<String> cleanup() {
                                        return null;
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void unrelatedCodeUnchanged() {
        rewriteRun(
                java(
                        """
                                package com.example;

                                public class UnrelatedService {
                                    public void doSomething() {
                                        System.out.println("Hello World");
                                    }

                                    public String executeBlocking(String input) {
                                        // Different executeBlocking method - should not be transformed
                                        return input.toUpperCase();
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void eventuallyWithUsedParameterUnchanged() {
        rewriteRun(
                // Stub classes
                java(
                        """
                                package java.util.function;
                                public interface Function<T, R> {
                                    R apply(T t);
                                }
                                """
                ),
                java(
                        """
                                package java.util.function;
                                public interface Supplier<T> {
                                    T get();
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.core;
                                public interface Future<T> {
                                    Future<T> eventually(java.util.function.Function<T, Future<T>> mapper);
                                    Future<T> eventually(java.util.function.Supplier<Future<T>> supplier);
                                }
                                """
                ),
                // Actual test case - eventually with used parameter should NOT be transformed
                java(
                        """
                                package com.example;

                                import io.vertx.core.Future;

                                public class FutureService {
                                    public Future<String> chainOperations(Future<String> future) {
                                        return future.eventually(result -> processResult(result));
                                    }

                                    private Future<String> processResult(String result) {
                                        return null;
                                    }
                                }
                                """
                )
        );
    }
}
