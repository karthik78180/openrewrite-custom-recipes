package com.rewrite.repoMaintenance;

import com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe;
import com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe;
import com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe;
import com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

import java.util.List;

/**
 * Top-level composite: bundles the JDK 25 upgrade with three repo-maintenance recipes.
 * Order: JDK upgrade → JSON edits → dependency bumps → format last.
 */
public class PostJava25UpgradeRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Upgrade to Java 25 and run repo maintenance";
    }

    @Override
    public @NotNull String getDescription() {
        return "Composite: Java 25 build/workflow upgrade, lambda.json updates, "
                + "dependency/plugin version bumps, and Checkstyle-derived autoformat.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new UpgradeToJava25Recipe(),
                new UpdateLambdaJsonRecipe(),
                new UpgradeProjectDependenciesRecipe(),
                new CheckstyleAutoFormatRecipe()
        );
    }
}
