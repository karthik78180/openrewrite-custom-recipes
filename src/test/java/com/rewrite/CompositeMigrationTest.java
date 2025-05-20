package com.rewrite;

import org.openrewrite.test.RewriteTest;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;


class CompositeMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/rewrite.yml", "com.example.CompositeMigration");
    }

    @Test
    void testConstantReferenceChange() {
        rewriteRun(
                java(
                        """
                        import com.old.Constants.MicroConstant;
        
                        public class Demo {
                            String id = MicroConstant.APP_ID;
                            String secret = MicroConstant.CLIENT_SECRET;
                            String user = MicroConstant.USER_NAME;
                        }
                        """,
                        """
                        import org.example.updated.ServerConstant;
        
                        public class Demo {
                            String id = ServerConstant.APP_ID;
                            String secret = ServerConstant.CLIENT_SECRET;
                            String user = ServerConstant.USERNAME;
                        }
                        """
                )
        );
    }

    @Test
    void testCompositeIncludesAddStructured() {
        rewriteRun(
                java(
                        """
                        package io.vertx.core;
        
                        public interface Promise<T> {
                        }
                        """
                ),
                // Mock Structured class
                java(
                        """
                        public class Structured {
                        }
                        """
                ),
                java(
                        """
                        import io.vertx.core.Promise;
        
                        public class MyVerticle {
                            public void start(Promise<String> init) {
                                System.out.println("Init called");
                            }
                        }
                        """,
                        """
                        import io.vertx.core.Promise;
        
                        public class MyVerticle {
                            public void start(Promise<String> init, Structured structured) {
                                System.out.println("Init called");
                            }
                        }
                        """
                )
        );
    }

    @Test
    void testCompositeIncludesAddStructured2() {
        rewriteRun(
                java(
                        """
                        package io.vertx.core;
        
                        public interface Promise<T> {
                        }
                        """
                ),
                // Mock Structured class
                java(
                        """
                        public class Structured {
                        }
                        """
                ),
                java(
                        """
                        import io.vertx.core.Promise;
        
                        public class MyVerticle {
                            public void start(Promise<Void> init) {
                                System.out.println("Init called");
                            }
                        }
                        """,
                        """
                        import io.vertx.core.Promise;
        
                        public class MyVerticle {
                            public void start(Promise<Void> init, Structured structured) {
                                System.out.println("Init called");
                            }
                        }
                        """
                )
        );
    }
}
