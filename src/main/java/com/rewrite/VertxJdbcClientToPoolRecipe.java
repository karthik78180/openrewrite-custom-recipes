package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

/**
 * Transforms Vert.x JDBCClient API calls to JDBCPool API.
 *
 * Changes:
 * - JDBCClient to JDBCPool type references
 * - JDBCClient.create() to JDBCPool.pool()
 * - SQLClient to JDBCPool where applicable
 *
 * Note: This recipe transforms the code but may leave unused imports.
 * The VertxJdbcImportMigration recipe (defined in rewrite.yml) handles
 * complete import cleanup using ChangeType.
 */
public class VertxJdbcClientToPoolRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Transform JDBCClient to JDBCPool";
    }

    @Override
    public String getDescription() {
        return "Replaces JDBCClient and SQLClient with JDBCPool, and updates create() calls to pool().";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<>() {

            // Transform variable type declarations from JDBCClient/SQLClient to JDBCPool
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);

                if (vd.getTypeExpression() instanceof J.Identifier id) {
                    String typeName = id.getSimpleName();

                    if ("JDBCClient".equals(typeName) || "SQLClient".equals(typeName)) {
                        J.Identifier newType = id.withSimpleName("JDBCPool")
                                .withType(JavaType.ShallowClass.build("io.vertx.jdbcclient.JDBCPool"));

                        vd = vd.withTypeExpression(newType);

                        maybeRemoveImport("io.vertx.ext.jdbc.JDBCClient");
                        maybeRemoveImport("io.vertx.ext.sql.SQLClient");
                        maybeAddImport("io.vertx.jdbcclient.JDBCPool");
                    }
                }

                return vd;
            }

            // Transform method invocations from JDBCClient.create() to JDBCPool.pool()
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

                // Check for JDBCClient.create() pattern
                if (mi.getSelect() instanceof J.Identifier select &&
                    "JDBCClient".equals(select.getSimpleName()) &&
                    "create".equals(mi.getSimpleName())) {

                    // Change select from JDBCClient to JDBCPool
                    J.Identifier newSelect = select.withSimpleName("JDBCPool")
                            .withType(JavaType.ShallowClass.build("io.vertx.jdbcclient.JDBCPool"));

                    // Change method name from create to pool
                    mi = mi.withSelect(newSelect)
                            .withName(mi.getName().withSimpleName("pool"));

                    maybeRemoveImport("io.vertx.ext.jdbc.JDBCClient");
                    maybeAddImport("io.vertx.jdbcclient.JDBCPool");
                }

                return mi;
            }
        };
    }
}
