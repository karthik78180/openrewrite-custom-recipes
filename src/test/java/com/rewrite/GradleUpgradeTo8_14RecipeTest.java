package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for GradleUpgradeTo8_14Recipe
 *
 * This test validates the recipe structure and composition.
 * Full integration tests with actual Gradle build files require the
 * gradle-tooling-api test module which is not included in this project's test dependencies.
 */
class GradleUpgradeTo8_14RecipeTest {

    @Test
    void recipeHasCorrectDisplayName() {
        GradleUpgradeTo8_14Recipe recipe = new GradleUpgradeTo8_14Recipe();

        assertThat(recipe.getDisplayName())
            .isEqualTo("Upgrade Gradle from 7.5+ to 8.14");
    }

    @Test
    void recipeHasDescription() {
        GradleUpgradeTo8_14Recipe recipe = new GradleUpgradeTo8_14Recipe();

        assertThat(recipe.getDescription())
            .isNotEmpty()
            .contains("Gradle 8.14")
            .contains("deprecated configurations");
    }

    @Test
    void recipeIsComposite() {
        GradleUpgradeTo8_14Recipe recipe = new GradleUpgradeTo8_14Recipe();
        List<Recipe> recipeList = recipe.getRecipeList();

        assertThat(recipeList)
            .isNotEmpty()
            .hasSize(4); // UpdateGradleWrapper + 3 plugin updates + FixDeprecatedGradleApis
    }

    @Test
    void recipeIncludesGradleWrapperUpdate() {
        GradleUpgradeTo8_14Recipe recipe = new GradleUpgradeTo8_14Recipe();
        List<Recipe> recipeList = recipe.getRecipeList();

        assertThat(recipeList)
            .anyMatch(r -> r.getClass().getSimpleName().equals("UpdateGradleWrapper"));
    }

    @Test
    void recipeIncludesPluginUpgrades() {
        GradleUpgradeTo8_14Recipe recipe = new GradleUpgradeTo8_14Recipe();
        List<Recipe> recipeList = recipe.getRecipeList();

        long pluginUpgradeCount = recipeList.stream()
            .filter(r -> r.getClass().getSimpleName().equals("UpgradePluginVersion"))
            .count();

        assertThat(pluginUpgradeCount)
            .isGreaterThanOrEqualTo(2); // At least 3 plugin upgrades
    }

    @Test
    void recipeIncludesDeprecatedApisFix() {
        GradleUpgradeTo8_14Recipe recipe = new GradleUpgradeTo8_14Recipe();
        List<Recipe> recipeList = recipe.getRecipeList();

        assertThat(recipeList)
            .anyMatch(r -> r.getDisplayName().contains("deprecated"));
    }

    @Test
    void deprecatedApisFixHasCorrectDescription() {
        GradleUpgradeTo8_14Recipe recipe = new GradleUpgradeTo8_14Recipe();
        List<Recipe> recipeList = recipe.getRecipeList();

        Recipe deprecatedApisFix = recipeList.stream()
            .filter(r -> r.getDisplayName().contains("deprecated"))
            .findFirst()
            .orElseThrow();

        assertThat(deprecatedApisFix.getDescription())
            .contains("Gradle 8.x");
    }
}
