package com.rewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

import java.util.List;

/**
 * Composite recipe containing all migration rules.
 *
 * This recipe combines multiple migration recipes into a single execution:
 * - Base class transformations (Vehicle to Car)
 * - Constant reference updates
 * - Vert.x JDBC client migration (3.9.16 to 5.0.5):
 *   - API changes from JDBCClient to JDBCPool
 *   - Import statement updates (handled via rewrite.yml as com.rewrite.VertxJdbcImportMigration)
 *
 * Note: Checkstyle-compliant formatting is applied via the YAML recipe (com.recipies.yaml.CheckstyleFormatting)
 * when using com.recipies.yaml.AllMigrations.
 */
public class Java21MigrationRecipes extends Recipe {
    @Override
    public @NotNull String getDisplayName() {
        return "Composite: All migration recipes";
    }

    @Override
    public @NotNull String getDescription() {
        return "Runs all migration recipes including base class changes, constant updates, and Vert.x JDBC migration.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new VehicleToCarRecipe(),
                new ChangeConstantReference(),
                new ChangeConstantsReference(),
                new VertxJdbcClientToPoolRecipe()
        );
    }
}