package com.rewrite;

import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.config.Environment;

public class GradleMigrationYamlRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return "Gradle migration from YAML";
    }

    @Override
    public String getDescription() {
        return "Activates the YAML-based Gradle wrapper and dependency updates";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Environment env = Environment.builder()
                .scanRuntimeClasspath()
                .build();
        return env.activateRecipes("com.mycompany.gradle.MigrateGradleSetup").getVisitor();
    }
}
