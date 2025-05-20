package com.rewrite;

import org.openrewrite.Recipe;
import org.openrewrite.config.Environment;

public class CompositeRecipe  extends Recipe {
    @Override
    public String getDisplayName() {
        return "Composite migration recipe";
    }

    @Override
    public String getDescription() {
        return "Runs custom constant refactoring and Gradle upgrade.";
    }

    public Recipe getDelegate() {
        return Environment.builder()
                .scanRuntimeClasspath()
                .build()
                .activateRecipes("com.example.CompositeMigration");
    }

    @Override
    public org.openrewrite.TreeVisitor<?, org.openrewrite.ExecutionContext> getVisitor() {
        // Delegates to the recipe list in the YAML
        return getDelegate().getVisitor();
    }
}
