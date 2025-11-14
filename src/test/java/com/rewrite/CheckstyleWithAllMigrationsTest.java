package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Integration tests for the complete AllMigrations YAML recipe.
 *
 * Tests the full recipe chain:
 * - Gradle upgrade to 8.13
 * - Java21MigrationRecipes (Vehicle->Car, constants, Vert.x JDBC)
 * - VertxJdbcMigrations (dependencies and imports)
 * - CheckstyleFormatting (imports, indentation, blank lines)
 */
class CheckstyleWithAllMigrationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/rewrite.yml", "com.recipies.yaml.AllMigrations");
    }

    @Test
    void vehicleToCarMigrationWithFormatting() {
        rewriteRun(
                java(
                        """
                                package com.example;

                                public class Vehicle {}
                                public class Car {}
                                public class MyVehicle extends Vehicle {}
                                
                                """,
                        """
                                package com.example;

                                public class Vehicle {
                                }

                                public class Car {
                                }

                                public class MyVehicle extends Car {
                                }

                                """
                )
        );
    }

    @Test
    void importsOrganizedIntoGroups() {
        rewriteRun(
                java(
                        """
                                package com.example;

                                import java.util.List;
                                import com.other.Utils;
                                import javax.swing.JFrame;

                                public class Example {
                                    public void method() {
                                        List<String> list = null;
                                        JFrame frame = null;
                                    }
                                }
                                """,
                        """
                                package com.example;

                                import com.other.Utils;

                                import javax.swing.JFrame;
                                import java.util.List;

                                public class Example {
                                    public void method() {
                                        List<String> list = null;
                                        JFrame frame = null;
                                    }
                                }

                                """
                )
        );
    }

    @Test
    void staticImportsSeparated() {
        rewriteRun(
                java(
                        """
                                package com.example;

                                import java.util.List;
                                import static java.lang.Math.max;
                                import java.io.File;
                                import static java.lang.Math.min;

                                public class Example {
                                    public void method() {
                                        int x = max(1, 2);
                                        int y = min(1, 2);
                                        List<String> list = null;
                                        File file = null;
                                    }
                                }

                                """,
                        """
                                package com.example;

                                import java.io.File;
                                import java.util.List;

                                import static java.lang.Math.max;
                                import static java.lang.Math.min;

                                public class Example {
                                    public void method() {
                                        int x = max(1, 2);
                                        int y = min(1, 2);
                                        List<String> list = null;
                                        File file = null;
                                    }
                                }

                                """
                )
        );
    }

    @Test
    void validCodeRemainsUnchanged() {
        rewriteRun(
                java(
                        """
                                package com.example;

                                import java.util.List;

                                public class Example {
                                    public void method() {
                                        List<String> list = null;
                                    }
                                }

                                """
                )
        );
    }
}
