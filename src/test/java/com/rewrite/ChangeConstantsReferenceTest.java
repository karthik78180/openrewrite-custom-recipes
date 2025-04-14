package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class ChangeConstantsReferenceTest implements RewriteTest {

    public JavaParser.Builder<?, ?> javaParser() {
        return JavaParser.fromJavaVersion();
    }

    @Test
    void replaceConstants() {
        rewriteRun(
            spec -> spec.recipe(new ChangeConstantsReference()),
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
