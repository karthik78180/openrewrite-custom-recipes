package com.rewrite.repoMaintenance.lambdaJson;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LambdaJsonConstantsTest {

    @Test
    void filePatternMatchesBothLayouts() {
        assertThat(LambdaJsonConstants.FILE_PATTERN).isEqualTo("**/config/*/lambda.json");
    }

    @Test
    void runtimeTargetIsJava25() {
        assertThat(LambdaJsonConstants.RUNTIME_PATH).isEqualTo("$.runtime");
        assertThat(LambdaJsonConstants.RUNTIME_VALUE).isEqualTo("java25");
    }

    @Test
    void handlerTargetIsExampleHandler() {
        assertThat(LambdaJsonConstants.HANDLER_PATH).isEqualTo("$.handler");
        assertThat(LambdaJsonConstants.HANDLER_VALUE).isEqualTo("com.example.Handler");
    }

    @Test
    void regionPathPreservesIntentionalMisspelling() {
        assertThat(LambdaJsonConstants.REGION_PATH).isEqualTo("$.deploymentCordinates.region");
        assertThat(LambdaJsonConstants.REGION_VALUE).isEqualTo("us-east-1");
    }

    @Test
    void deletePathsTargetTopLevelKeys() {
        assertThat(LambdaJsonConstants.DELETE_FUNCTION_VERSION_PATH).isEqualTo("$.functionVersion");
        assertThat(LambdaJsonConstants.DELETE_VERSION_PATH).isEqualTo("$.version");
    }
}
