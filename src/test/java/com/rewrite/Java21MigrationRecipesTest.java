package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class Java21MigrationRecipesTest implements RewriteTest {

    public JavaParser.Builder<?, ?> javaParser() {
        return JavaParser.fromJavaVersion();
    }

    @Test
    void testCompositeMigrationforConstants() {
        rewriteRun(
            spec -> spec.recipe(new Java21MigrationRecipes()),
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

    @Test
    void classExtendingVehicleIsUpdated() {
        rewriteRun(
                spec -> spec.recipe(new Java21MigrationRecipes()),
                java(
                        """
                        package com.example;
        
                        public class Vehicle {}
                        public class Car {}
                        public class MyVehicle extends Vehicle {}
                        """,
                        """
                        package com.example;
        
                        public class Vehicle {}
                        public class Car {}
                        public class MyVehicle extends Car {}
                        """
                )
        );
    }

    @Test
    void unrelatedClassIsUnchanged() {
        rewriteRun(
                java(
                        """
                        package com.example;
        
                        public class SomethingElse {
                            void doNothing() {}
                        }
                        """
                )
        );
    }

    @Test
    void testFullMigrationRecipe() {
        spec.recipe(new FullPlatformMigrationRecipe())
                .parser(JavaParser.fromJavaVersion().build());

        spec.run(
                java(
                        """
                                package com.demo;
                                import com.example.Vehicle;
                                
                                public class MyClass extends Vehicle {
                                }
                                """,
                        javaSourceSpec -> javaSourceSpec.afterRecipe(cu -> {
                            String code = cu.printAll();
                            assertThat(code).contains("extends Car");
                            assertThat(code).contains("import com.example.Car");
                        })
                ),
                java(
                        """
                                package com.demo;
                                import com.old.Constants.MicroConstant;
                                
                                class Test {
                                    String val = MicroConstant.USER_NAME;
                                }
                                """,
                        javaSourceSpec -> javaSourceSpec.afterRecipe(cu -> {
                            String code = cu.printAll();
                            assertThat(code).contains("ServerConstant.USERNAME");
                            assertThat(code).contains("import org.example.updated.ServerConstant");
                        })
                ),
                gradleProperties(
                        """
                                distributionUrl=https\\://services.gradle.org/distributions/gradle-7.0-all.zip
                                """,
                        specBlock -> specBlock.afterRecipe(file -> {
                            String content = file.getText();
                            assertThat(content).contains("gradle-8.13");
                        })
                )
        );
    }
}
