package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class VertxJdbcMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // Test the client-to-pool recipe directly since VertxJdbcImportMigration
        // uses ChangeType which requires actual classes on classpath (not stubs)
        spec.recipe(new VertxJdbcClientToPoolRecipe());
    }

    @Test
    void jdbcClientCreateTransformedToJdbcPoolPool() {
        rewriteRun(
                // Stub classes - no changes expected (only "before" provided)
                java(
                        """
                                package io.vertx.core;
                                public interface Vertx {}
                                """
                ),
                java(
                        """
                                package io.vertx.core.json;
                                public class JsonObject {}
                                """
                ),
                java(
                        """
                                package io.vertx.ext.jdbc;
                                public class JDBCClient {
                                    public static JDBCClient create(Object vertx, Object config) {
                                        return null;
                                    }
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.jdbcclient;
                                public class JDBCPool {
                                    public static JDBCPool pool(Object vertx, Object config) {
                                        return null;
                                    }
                                }
                                """
                ),
                // Actual test case - with before/after transformation
                // Note: The JDBCClient import remains because maybeRemoveImport is called
                // but only removes imports that aren't referenced in the code anymore
                java(
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.json.JsonObject;
                                import io.vertx.ext.jdbc.JDBCClient;

                                public class DatabaseService {
                                    public void init(Vertx vertx, JsonObject config) {
                                        JDBCClient client = JDBCClient.create(vertx, config);
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.core.json.JsonObject;
                                import io.vertx.ext.jdbc.JDBCClient;
                                import io.vertx.jdbcclient.JDBCPool;

                                public class DatabaseService {
                                    public void init(Vertx vertx, JsonObject config) {
                                        JDBCPool client = JDBCPool.pool(vertx, config);
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void sqlClientTypeTransformedToJdbcPool() {
        rewriteRun(
                // Stub classes - no changes expected (only "before" provided)
                java(
                        """
                                package io.vertx.core;
                                public interface Vertx {}
                                """
                ),
                java(
                        """
                                package io.vertx.ext.sql;
                                public interface SQLClient {}
                                """
                ),
                java(
                        """
                                package io.vertx.ext.jdbc;
                                public class JDBCClient {
                                    public static JDBCClient create(Object vertx, Object config) {
                                        return null;
                                    }
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.jdbcclient;
                                public class JDBCPool {
                                    public static JDBCPool pool(Object vertx, Object config) {
                                        return null;
                                    }
                                }
                                """
                ),
                // Actual test case - SQLClient type should be transformed to JDBCPool
                // In Vert.x 5, SQLClient is deprecated and merged into JDBCPool
                // Note: This recipe transforms types/method calls but VertxJdbcImportMigration
                // (in rewrite.yml) handles complete import cleanup
                java(
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.ext.sql.SQLClient;
                                import io.vertx.ext.jdbc.JDBCClient;

                                public class Repository {
                                    private SQLClient client;

                                    public void setup(Vertx vertx) {
                                        client = JDBCClient.create(vertx, null);
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.ext.sql.SQLClient;
                                import io.vertx.jdbcclient.JDBCPool;

                                public class Repository {
                                    private JDBCPool client;

                                    public void setup(Vertx vertx) {
                                        client = JDBCPool.pool(vertx, null);
                                    }
                                }
                                """
                )
        );
    }

    @Test
    void multipleJdbcClientReferencesTransformed() {
        rewriteRun(
                // Stub classes - no changes expected (only "before" provided)
                java(
                        """
                                package io.vertx.core;
                                public interface Vertx {}
                                """
                ),
                java(
                        """
                                package io.vertx.ext.jdbc;
                                public class JDBCClient {
                                    public static JDBCClient create(Object vertx, Object config) {
                                        return null;
                                    }
                                }
                                """
                ),
                java(
                        """
                                package io.vertx.jdbcclient;
                                public class JDBCPool {
                                    public static JDBCPool pool(Object vertx, Object config) {
                                        return null;
                                    }
                                }
                                """
                ),
                // Actual test case
                // Note: The JDBCClient import remains in the output
                java(
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.ext.jdbc.JDBCClient;

                                public class MultiClientService {
                                    private JDBCClient primaryClient;
                                    private JDBCClient secondaryClient;

                                    public void init(Vertx vertx) {
                                        primaryClient = JDBCClient.create(vertx, null);
                                        secondaryClient = JDBCClient.create(vertx, null);
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import io.vertx.core.Vertx;
                                import io.vertx.ext.jdbc.JDBCClient;
                                import io.vertx.jdbcclient.JDBCPool;

                                public class MultiClientService {
                                    private JDBCPool primaryClient;
                                    private JDBCPool secondaryClient;

                                    public void init(Vertx vertx) {
                                        primaryClient = JDBCPool.pool(vertx, null);
                                        secondaryClient = JDBCPool.pool(vertx, null);
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
                                }
                                """
                )
        );
    }
}
