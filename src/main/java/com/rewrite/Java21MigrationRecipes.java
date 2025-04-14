package com.rewrite;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

import java.util.List;

/**
 * Composite recipe containing all migration rules
 */
public class Java21MigrationRecipes extends Recipe {
    @Override
    public @NotNull String getDisplayName() {
        return "Composite: Change base classes";
    }

    @Override
    public @NotNull String getDescription() {
        return "Runs all base class migration rules as a composite recipe.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new VehicleToCarRecipe(),
                new ChangeConstantReference(),
                new ChangeConstantsReference()
        );
    }
}