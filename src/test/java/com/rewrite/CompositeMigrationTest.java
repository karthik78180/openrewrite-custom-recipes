package com.rewrite;

import org.openrewrite.test.RewriteTest;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;


class CompositeMigrationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/rewrite.yml", "com.recipies.yaml.AllMigrations");
    }

    @Test
    void testConstantReferenceChange() {
        rewriteRun(
                java(
                        """
                        import com.old.Constants.MicroConstant;
        
                        public class Demo {
                            String id = MicroConstant.APP_ID;
                            String secret = MicroConstant.CLIENT_SECRET;
                            String user = MicroConstant.USER_NAME;
                        }
                        """,
                        """
                        import org.example.updated.ServerConstant;
        
                        public class Demo {
                            String id = ServerConstant.APP_ID;
                            String secret = ServerConstant.CLIENT_SECRET;
                            String user = ServerConstant.USERNAME;
                        }
                        """
                )
        );
    }
}
