package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ChangeConstantReferenceTest implements RewriteTest {

    // @Override
    public JavaParser.Builder<?, ?> javaParser() {
        return JavaParser.fromJavaVersion();
    }

    @Test
    void shouldReplaceMicroConstantAppIdWithServerConstant() {
        rewriteRun(
            spec -> spec.recipe(new ChangeConstantReference()),
            java(
                """
                package com.demo;

                import com.old.Constants.MicroConstant;

                public class MyClass {
                    public void test() {
                        String id = MicroConstant.APP_ID;
                    }
                }
                """,
                """
                package com.demo;

                import org.example.updated.ServerConstant;

                public class MyClass {
                    public void test() {
                        String id = ServerConstant.APP_ID;
                    }
                }
                """
            )
        );
    }
}
