package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

/**
 * Test suite for Checkstyle formatting recipes.
 *
 * Tests the YAML CheckstyleFormatting recipe which uses OpenRewrite's built-in recipes:
 * - AutoFormat (standard Java formatting)
 * - BlankLines (max 1 consecutive blank line)
 * - OrderImports (import organization with groups: *, javax, java)
 */
class CheckstyleFormattingRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/rewrite.yml", "com.recipies.yaml.CheckstyleFormatting");
    }

    @Test
    void maxConsecutiveBlankLinesEnforced() {
        // Tests that BlankLines recipe reduces multiple consecutive blank lines
        // 3 blank lines between fields are reduced to 2
        // 3 blank lines between methods are reduced to 2
        rewriteRun(
                java(
                        """
                                package com.example;

                                public class Example {
                                    private String field1;



                                    private String field2;

                                    public void method1() {
                                    }



                                    public void method2() {
                                    }
                                }
                                    
                                """,
                        """
                                package com.example;

                                public class Example {
                                    private String field1;


                                    private String field2;

                                    public void method1() {
                                    }


                                    public void method2() {
                                    }
                                }

                                """
                )
        );
    }

    @Test
    void singleBlankLineIsPreserved() {
        rewriteRun(
                java(
                        """
                                package com.example;

                                public class Example {

                                    public void method() {
                                    }
                                }

                                """
                )
        );
    }

    @Test
    void importsAreOrganizedByGroup() {
        rewriteRun(
                java(
                        """
                                package com.example;

                                import java.util.List;
                                import com.other.Utils;
                                import javax.swing.JFrame;
                                import java.io.File;

                                public class Example {
                                    public void method() {
                                        List<String> list = null;
                                        JFrame frame = null;
                                        File file = null;
                                    }
                                }

                                """,
                        """
                                package com.example;

                                import com.other.Utils;

                                import javax.swing.JFrame;
                                import java.io.File;
                                import java.util.List;

                                public class Example {
                                    public void method() {
                                        List<String> list = null;
                                        JFrame frame = null;
                                        File file = null;
                                    }
                                }

                                """
                )
        );
    }

    @Test
    void staticImportsSeparatedAndAlphabetized() {
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
    void formattingPreservesValidCode() {
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
