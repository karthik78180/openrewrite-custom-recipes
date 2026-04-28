package com.rewrite.repoMaintenance.lambdaJson;

/** Hardcoded keys, target values, and file pattern for the lambda.json updater. */
public final class LambdaJsonConstants {

    private LambdaJsonConstants() {
    }

    /** Glob covering single-module (config/{name}/lambda.json) and monorepo ({module}/config/{name}/lambda.json). */
    public static final String FILE_PATTERN = "**/config/*/lambda.json";

    public static final String RUNTIME_PATH = "$.runtime";
    public static final String RUNTIME_VALUE = "java25";

    public static final String HANDLER_PATH = "$.handler";
    public static final String HANDLER_VALUE = "com.example.Handler";

    // Spelling "Cordinates" intentional — matches the actual JSON key in target lambda.json files.
    public static final String REGION_PATH = "$.deploymentCordinates.region";
    public static final String REGION_VALUE = "us-east-1";

    public static final String DELETE_FUNCTION_VERSION_PATH = "$.functionVersion";
    public static final String DELETE_VERSION_PATH = "$.version";
}
