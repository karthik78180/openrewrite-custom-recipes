package com.rewrite.repoMaintenance.checkstyle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class CheckstyleAutoFormatRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CheckstyleAutoFormatRecipe());
    }

    @Test
    void importsReorderedIntoGroups() {
        rewriteRun(
                spec -> spec.typeValidationOptions(TypeValidation.none()),
                java(
                        """
                        package com.example;

                        import java.util.List;
                        import com.acme.Other;
                        import javax.annotation.Nullable;

                        public class A {
                          public List<String> a;
                          public Other o;
                          public Nullable n;
                        }
                        """,
                        // OrderImports groups third-party (com.acme) ahead of javax and java, but
                        // treats javax.* and java.* as a single combined group with no blank line
                        // between them. AutoFormat reformats indentation to 4 spaces and adds a
                        // trailing newline.
                        """
                        package com.example;

                        import com.acme.Other;

                        import javax.annotation.Nullable;
                        import java.util.List;

                        public class A {
                            public List<String> a;
                            public Other o;
                            public Nullable n;
                        }

                        """
                )
        );
    }

    @Test
    void trailingWhitespaceRemoved() {
        rewriteRun(
                java(
                        "package com.example;   \n\npublic class A {}\n",
                        // RemoveTrailingWhitespace strips spaces after ';'. AutoFormat (IntelliJ
                        // defaults) expands '{}' to '{\n}'. EmptyNewlineAtEndOfFile adds '\n'.
                        // The extra blank line before """ keeps the trailing '\n' after trimIndent.
                        """
                        package com.example;

                        public class A {
                        }

                        """
                )
        );
    }

    @Test
    void multipleBlankLinesCollapsed() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class A {
                            public int x = 1;



                            public int y = 2;
                        }
                        """,
                        // BlankLines (default config keepMaximum.inDeclarations=2) collapses 3+
                        // consecutive blank lines inside a class body down to 2. AutoFormat adds a
                        // blank line after the package statement. EmptyNewlineAtEndOfFile ensures a
                        // trailing newline. The extra blank line before """ keeps the '\n' after
                        // RewriteTest's trimIndentPreserveCRLF strips the outermost trailing '\n'.
                        "package com.example;\n\npublic class A {\n    public int x = 1;\n\n\n    public int y = 2;\n}\n\n"
                )
        );
    }

    @Test
    void newlineAtEndOfFileAdded() {
        rewriteRun(
                java(
                        "package com.example;\npublic class A {}",
                        // AutoFormat (IntelliJ defaults) inserts a blank line after the package
                        // statement and expands '{}' to '{\n}'. EmptyNewlineAtEndOfFile adds '\n'.
                        // The extra blank line before """ keeps the trailing '\n' after trimIndent.
                        """
                        package com.example;

                        public class A {
                        }

                        """
                )
        );
    }

    @Test
    void modifierOrderCorrected() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class A {
                          static public final int X = 1;
                        }
                        """,
                        // AutoFormat (IntelliJ defaults) adds a blank line after package and
                        // reformats indentation to 4 spaces. EmptyNewlineAtEndOfFile ensures a
                        // trailing newline. The extra blank line before """ keeps the \n after
                        // RewriteTest's trimIndentPreserveCRLF strips the outermost trailing \n.
                        """
                        package com.example;

                        public class A {
                            public static final int X = 1;
                        }

                        """
                )
        );
    }

    @Disabled("RedundantModifier is not available in rewrite-static-analysis 2.11.0; "
            + "the class org.openrewrite.staticanalysis.RedundantModifier does not exist in this BOM. "
            + "Upgrade rewrite-static-analysis to a version that includes RedundantModifier, or "
            + "substitute with an equivalent recipe — see docs/CheckstyleAutoFormatRecipe.md (Known Limitations).")
    @Test
    void redundantPublicOnInterfaceMethodRemoved() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public interface I {
                          public void doIt();
                        }
                        """,
                        """
                        package com.example;
                        public interface I {
                          void doIt();
                        }
                        """
                )
        );
    }

    @Disabled("AutoFormat does not load JAR-shipped style.yml in RewriteTest classpath; "
            + "verified manually via publishToMavenLocal — see docs/CheckstyleAutoFormatRecipe.md (Known Limitations).")
    @Test
    void indentationReformattedToTwoSpaces() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class A {
                            public void m() {
                                int x = 1;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class A {
                          public void m() {
                            int x = 1;
                          }
                        }
                        """
                )
        );
    }

    @Test
    void simplifyBooleanComparisonToTrue() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class A {
                          public boolean m(boolean x) {
                            if (x == true) { return true; }
                            return false;
                          }
                        }
                        """,
                        // AutoFormat (IntelliJ defaults) adds a blank line after package and
                        // reformats indentation to 4 spaces. EmptyNewlineAtEndOfFile ensures a
                        // trailing newline. SimplifyBooleanExpression + SimplifyBooleanReturn
                        // collapse the if-return pattern into a single return statement.
                        // The extra blank line before """ keeps \n after trimIndentPreserveCRLF.
                        """
                        package com.example;

                        public class A {
                            public boolean m(boolean x) {
                                return x;
                            }
                        }

                        """
                )
        );
    }

    @Test
    void alreadyFormattedFileIsNoOp() {
        rewriteRun(
                java(
                        """
                        package com.example;

                        public class A {
                            public int x = 1;
                        }

                        """
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        CheckstyleAutoFormatRecipe recipe = new CheckstyleAutoFormatRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("Checkstyle");
        List<Recipe> sub = recipe.getRecipeList();
        // 11 sub-recipes: RedundantModifier omitted because it is absent from
        // rewrite-static-analysis 2.11.0 (the version declared in libs.versions.toml).
        assertThat(sub).hasSize(11);
    }
}
