package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

/**
 * Tests for VertxJdbcMigration recipe loaded from rewrite.yml.
 * This verifies that the YAML recipe configuration works correctly.
 *
 * Note: Code transformation tests are in VertxJdbcMigrationRecipeTest.
 * This test focuses on dependency migrations using Gradle.
 */
class RewriteYamlFileTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // Load the recipe from the YAML file
        spec.recipe(
            org.openrewrite.config.Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes("com.recipies.yaml.VertxJdbcMigrations")
        );
    }

    @Test
    void upgradesVertxJdbcClientInGradleBuild() {
        rewriteRun(
                buildGradle(
                        """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    implementation 'io.vertx:vertx-jdbc-client:3.9.16'
                                }
                                """,
                        """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    implementation 'io.vertx:vertx-jdbc-client:5.0.5'
                                }
                                """
                )
        );
    }

    @Test
    void handlesMultipleVertxDependencies() {
        rewriteRun(
                buildGradle(
                        """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    implementation 'io.vertx:vertx-jdbc-client:3.9.16'
                                    implementation 'io.vertx:vertx-core:3.9.16'
                                }
                                """,
                        """
                                plugins {
                                    id 'java'
                                }

                                dependencies {
                                    implementation 'io.vertx:vertx-jdbc-client:5.0.5'
                                    implementation 'io.vertx:vertx-core:5.0.5'
                                }
                                """
                )
        );
    }

    @Test
    void compositeMigrationIncludesVertxRecipe() {
        // Verify that the composite recipe includes VertxJdbcMigrations
        var environment = org.openrewrite.config.Environment.builder()
                .scanRuntimeClasspath()
                .build();

        var compositeRecipe = environment.activateRecipes("com.recipies.yaml.AllMigrations");
        var recipeList = compositeRecipe.getRecipeList();

        // Check that VertxJdbcMigrations is part of the composite
        boolean hasVertxMigration = recipeList.stream()
                .anyMatch(recipe -> recipe.getName().equals("com.recipies.yaml.VertxJdbcMigrations"));

        org.junit.jupiter.api.Assertions.assertTrue(
                hasVertxMigration,
                "AllMigrations should include VertxJdbcMigrations recipe"
        );
    }

    @Test
    void yamlRecipeCanBeLoaded() {
        // Verify that the recipe can be loaded from YAML
        var environment = org.openrewrite.config.Environment.builder()
                .scanRuntimeClasspath()
                .build();

        var recipe = environment.activateRecipes("com.recipies.yaml.VertxJdbcMigrations");

        org.junit.jupiter.api.Assertions.assertNotNull(recipe, "Recipe should be loadable from YAML");
        org.junit.jupiter.api.Assertions.assertEquals(
                "com.recipies.yaml.VertxJdbcMigrations",
                recipe.getName(),
                "Recipe name should match"
        );
        org.junit.jupiter.api.Assertions.assertEquals(
                "Migrate Vert.x JDBC from 3.9.16 to 5.0.5",
                recipe.getDisplayName(),
                "Display name should match"
        );
    }
}
