package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.gradleProperties;

class GradleMigrationYamlRecipeTest {

    @Test
    void updatesGradleWrapperAndDependencies(RewriteSpec spec) {
        spec.recipe(new GradleMigrationYamlRecipe());

        spec.run(
            gradleProperties(
                // Simulating old wrapper version in gradle-wrapper.properties
                """
                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.0-all.zip
                """,
                propsSpec -> propsSpec.afterRecipe(file -> {
                    String updated = file.getText();
                    assertThat(updated).contains("gradle-8.13");
                })
            )
        );
    }
}
