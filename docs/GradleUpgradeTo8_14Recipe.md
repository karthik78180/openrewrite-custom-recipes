# GradleUpgradeTo8_14Recipe

This OpenRewrite recipe automates the upgrade of Gradle projects from version 7.5+ to version 8.14.

---

## ✅ Features

- **Gradle Wrapper Update**: Automatically updates `gradle-wrapper.properties` to use Gradle 8.14
- **Deprecated Configuration Replacement**: Replaces deprecated dependency configurations:
  - `compile` → `implementation`
  - `testCompile` → `testImplementation`
  - `runtime` → `runtimeOnly`
  - `testRuntime` → `testRuntimeOnly`
- **Plugin Version Updates**: Updates common plugins to Gradle 8.14-compatible versions
- **Build Script Compatibility**: Handles both Groovy (`.gradle`) and Kotlin (`.gradle.kts`) build scripts

---

## 📌 Use Case

When upgrading projects from Gradle 7.x to Gradle 8.14, many APIs have been deprecated or removed. This recipe automates the migration process, ensuring:
- Your Gradle wrapper is updated to the target version
- Deprecated dependency configurations are replaced with modern equivalents
- Plugin versions are compatible with Gradle 8.14
- Your build scripts follow current best practices

---

## 🔄 Migration Details

### Gradle Version Support

- **Minimum Source Version**: Gradle 7.5+
- **Target Version**: Gradle 8.14

### Key Changes in Gradle 8.x

1. **Removed Configurations**: The legacy dependency configurations (`compile`, `runtime`, etc.) were deprecated in Gradle 5.x and removed in Gradle 7.x. This recipe ensures any remaining usages are updated.

2. **Wrapper Updates**: Updates the Gradle wrapper distribution URL and adds new properties for improved security:
   - `networkTimeout=10000`
   - `validateDistributionUrl=true`

3. **Plugin Compatibility**: Common plugins are updated to versions compatible with Gradle 8.14:
   - Shadow Plugin: 8.1.1+
   - Spring Dependency Management: 1.1.4+
   - Spring Boot: 3.2.0+

---

## ⚙️ How It Works

This recipe is a **composite recipe** that combines multiple transformations:

1. **UpdateGradleWrapper**: Updates `gradle/wrapper/gradle-wrapper.properties` to reference Gradle 8.14
2. **UpgradePluginVersion**: Updates plugin versions to Gradle 8.14-compatible releases
3. **FixDeprecatedGradleApis**: Custom visitor that:
   - Parses Gradle build files (both `.gradle` and `.gradle.kts`)
   - Identifies deprecated method invocations
   - Replaces them with modern equivalents

### Implementation Details

The recipe uses:
- `GroovyIsoVisitor` to traverse Gradle build scripts
- AST pattern matching to identify deprecated API usage
- Safe replacements that preserve build behavior

---

## 🧩 Usage

### Option 1: Using OpenRewrite Gradle Plugin

Add to your `build.gradle`:

```gradle
plugins {
    id 'org.openrewrite.rewrite' version '6.25.0'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    rewrite 'com.rewrite:openrewrite-custom-recipes:1.0.0'
}

rewrite {
    activeRecipe 'com.rewrite.GradleUpgradeTo8_14Recipe'
}
```

Then run:
```bash
./gradlew rewriteRun
```

### Option 2: Using YAML Configuration

Create `rewrite.yml` in your project root:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.yourcompany.UpgradeToGradle8
displayName: Upgrade to Gradle 8.14
recipeList:
  - com.rewrite.GradleUpgradeTo8_14Recipe
```

---

## 📋 Before and After Examples

### Example 1: Dependency Configurations

**Before:**
```gradle
dependencies {
    compile 'org.springframework:spring-core:5.3.20'
    testCompile 'junit:junit:4.13.2'
    runtime 'com.h2database:h2:2.1.214'
}
```

**After:**
```gradle
dependencies {
    implementation 'org.springframework:spring-core:5.3.20'
    testImplementation 'junit:junit:4.13.2'
    runtimeOnly 'com.h2database:h2:2.1.214'
}
```

### Example 2: Gradle Wrapper Properties

**Before:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-7.6-bin.zip
```

**After:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14-bin.zip
networkTimeout=10000
validateDistributionUrl=true
```

---

## ⚠️ Important Notes

### Manual Steps Required

While this recipe automates most of the upgrade process, you may need to handle these manually:

1. **Build Scan Plugin**: If using the Build Scan plugin, ensure you're using version 3.0+
2. **Custom Plugins**: Third-party plugins may need version updates beyond what this recipe provides
3. **Build Logic**: Complex custom build logic may require manual review
4. **Testing**: Always test your build thoroughly after migration:
   ```bash
   ./gradlew clean build --no-build-cache
   ```

### Gradle 8.x Breaking Changes

Be aware of these key breaking changes in Gradle 8.x:

- Java 8 is the minimum required version for running Gradle
- Some deprecated APIs from Gradle 7.x have been removed
- Configuration cache is more strict
- Build cache key generation has changed

### Incremental Upgrade Path

If upgrading from very old Gradle versions (< 7.0), consider an incremental approach:
1. Gradle 6.x → Gradle 7.6
2. Gradle 7.6 → Gradle 8.14

---

## 🔗 References

- [Gradle 8.14 Release Notes](https://docs.gradle.org/8.14/release-notes.html)
- [Gradle 8.x Upgrade Guide](https://docs.gradle.org/current/userguide/upgrading_version_8.html)
- [Dependency Management for Java Projects](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph)
- [OpenRewrite Gradle Recipes](https://docs.openrewrite.org/recipes/gradle)

---

## 🧪 Testing

The recipe includes comprehensive tests covering:
- Gradle wrapper version updates
- Deprecated configuration replacements (`compile`, `runtime`, etc.)
- Multiple configurations in a single build file
- Both Groovy and Kotlin DSL
- Build files without deprecated APIs (no-op scenarios)

See `GradleUpgradeTo8_14RecipeTest.java` for complete test coverage.

---

## 🤝 Contributing

Found an edge case or want to improve the recipe? Contributions are welcome! Please see the project's `CONTRIBUTING.md` for guidelines.
