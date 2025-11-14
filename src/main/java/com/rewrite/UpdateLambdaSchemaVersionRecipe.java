package com.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This OpenRewrite recipe updates the schemaVersion field in lambda.json files
 * located in config/*&#47;lambda.json paths to "2.12.0" if it's not already set to that value.
 */
public class UpdateLambdaSchemaVersionRecipe extends Recipe {

    private static final String TARGET_VERSION = "2.12.0";

    @Override
    public String getDisplayName() {
        return "Update Lambda Config Schema Version to 2.12.0";
    }

    @Override
    public String getDescription() {
        return "Updates the schemaVersion field in config/*/lambda.json files to 2.12.0 if it's not already set to that value.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JsonIsoVisitor<ExecutionContext>() {

            @Override
            public Json.Document visitDocument(Json.Document document, ExecutionContext ctx) {
                // Get the source path of the current file
                String sourcePath = document.getSourcePath().toString();

                // Check if this file matches the pattern config/*/lambda.json
                if (!isLambdaConfigFile(sourcePath)) {
                    return document;
                }

                // Process the document
                return super.visitDocument(document, ctx);
            }

            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                // Check if this is the schemaVersion key
                if (member.getKey() instanceof Json.Literal key &&
                    "schemaVersion".equals(key.getValue())) {

                    // Check if the value is not already "2.12.0"
                    if (member.getValue() instanceof Json.Literal value) {
                        Object currentValue = value.getValue();

                        // If the current value is not "2.12.0", update it
                        if (!TARGET_VERSION.equals(currentValue)) {
                            Json.Literal newValue = value.withValue(TARGET_VERSION);
                            return member.withValue(newValue);
                        }
                    }
                }

                return super.visitMember(member, ctx);
            }

            /**
             * Checks if the file path matches the pattern config/*&#47;lambda.json
             *
             * @param sourcePath The source path of the file
             * @return true if the path matches the pattern, false otherwise
             */
            private boolean isLambdaConfigFile(String sourcePath) {
                // Normalize the path to use forward slashes
                String normalizedPath = sourcePath.replace('\\', '/');

                // Check if the path contains config/ followed by any directory, then lambda.json
                // Matches: config/someName1/lambda.json, config/someName2/lambda.json, etc.
                if (normalizedPath.contains("config/") && normalizedPath.endsWith("/lambda.json")) {
                    // Extract the part after "config/"
                    int configIndex = normalizedPath.indexOf("config/");
                    String afterConfig = normalizedPath.substring(configIndex + "config/".length());

                    // Check that there's exactly one directory level between config and lambda.json
                    // i.e., config/someName/lambda.json (not config/lambda.json or config/a/b/lambda.json)
                    String[] parts = afterConfig.split("/");
                    return parts.length == 2 && "lambda.json".equals(parts[1]);
                }

                return false;
            }
        };
    }
}
