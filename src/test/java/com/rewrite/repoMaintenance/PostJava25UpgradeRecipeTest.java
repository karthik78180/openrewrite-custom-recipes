package com.rewrite.repoMaintenance;

import com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe;
import com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe;
import com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe;
import com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.json.Assertions.json;

class PostJava25UpgradeRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PostJava25UpgradeRecipe());
    }

    @Test
    void smokeTestAcrossAllFourLegs() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        java {
                            toolchain { languageVersion = JavaLanguageVersion.of(21) }
                        }
                        dependencies {
                            implementation 'io.vertx:vertx-core:3.9.16'
                        }
                        """,
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        java {
                            toolchain { languageVersion = JavaLanguageVersion.of(25) }
                        }
                        dependencies {
                            implementation 'io.vertx:vertx-core:5.0.5'
                        }
                        """
                ),
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler",
                          "functionVersion": "5",
                          "version": "1.0",
                          "deploymentCordinates": { "region": "eu-west-1" }
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": { "region": "us-east-1" }
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                ),
                java(
                        """
                        package com.example;
                        public class A {
                            static public final int X = 1;
                        }
                        """,
                        """
                        package com.example;

                        public class A {
                            public static final int X = 1;
                        }

                        """
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        PostJava25UpgradeRecipe recipe = new PostJava25UpgradeRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("Java 25");
        List<Recipe> sub = recipe.getRecipeList();
        assertThat(sub).hasSize(4);
        assertThat(sub).anyMatch(r -> r instanceof UpgradeToJava25Recipe);
        assertThat(sub).anyMatch(r -> r instanceof UpdateLambdaJsonRecipe);
        assertThat(sub).anyMatch(r -> r instanceof UpgradeProjectDependenciesRecipe);
        assertThat(sub).anyMatch(r -> r instanceof CheckstyleAutoFormatRecipe);
    }
}
