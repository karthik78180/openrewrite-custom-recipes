package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

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
}
