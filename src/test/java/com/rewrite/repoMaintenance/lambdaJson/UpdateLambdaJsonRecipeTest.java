package com.rewrite.repoMaintenance.lambdaJson;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;

class UpdateLambdaJsonRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateLambdaJsonRecipe());
    }

    @Test
    void singleModuleLayout() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler",
                          "functionVersion": "5",
                          "version": "1.0",
                          "deploymentCordinates": {
                            "region": "eu-west-1"
                          }
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void monorepoLayout() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler",
                          "functionVersion": "5",
                          "version": "1.0",
                          "deploymentCordinates": {
                            "region": "eu-west-1"
                          }
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        spec -> spec.path("services/api/config/api/lambda.json")
                )
        );
    }

    @Test
    void doesNotTouchJsonOutsidePattern() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "functionVersion": "5"
                        }
                        """,
                        spec -> spec.path("some/other/lambda.json")
                )
        );
    }

    @Test
    void noOpWhenAlreadyCorrect() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void deploymentCoordinatesAbsent_topLevelStillUpdates() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler"
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler"
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void partialUpdateOnlyTouchesIncorrectKeys() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "java25",
                          "handler": "old.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void deletesOnlyVersionWhenFunctionVersionAbsent() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "version": "1.2",
                          "handler": "old.Handler"
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler"
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void preservesUnrelatedTopLevelKeys() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler",
                          "memory": 512,
                          "timeout": 30
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "memory": 512,
                          "timeout": 30
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        UpdateLambdaJsonRecipe recipe = new UpdateLambdaJsonRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("lambda.json");
        List<Recipe> sub = recipe.getRecipeList();
        // 3 ChangeValue + 2 DeleteKey = 5 sub-recipes
        assertThat(sub).hasSize(5);
    }
}
