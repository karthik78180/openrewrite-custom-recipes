# ChangeConstantsReference

An OpenRewrite recipe that updates constant references from one class to another using a predefined mapping.

## Overview

This recipe replaces constant references from the old class `MicroConstant` to the new class `ServerConstant`, including:

- Rewriting static field references (e.g., `MicroConstant.APP_ID` → `ServerConstant.APP_ID`)
- Updating the import statement for the constants class
- Applying a custom mapping for renamed constants (e.g., `USER_NAME` → `USERNAME`)

## Example Transformation

### Before

```java
import com.old.Constants.MicroConstant;

String user = MicroConstant.USER_NAME;
```
### After
```java
import org.example.updated.ServerConstant;

String user = ServerConstant.USERNAME;

```

## Configuration

This recipe is preconfigured with the following values:

- **Old constant class:** `MicroConstant`
- **New constant class:** `ServerConstant`
- **Old import path:** `com.old.Constants.MicroConstant`
- **New import path:** `org.example.updated.ServerConstant`

### Constant Field Mappings

| Old Constant     | New Constant   |
|------------------|----------------|
| `APP_ID`         | `APP_ID`       |
| `CLIENT_SECRET`  | `CLIENT_SECRET`|
| `USER_NAME`      | `USERNAME`     |

Only constants listed above will be transformed. Others remain untouched.

---

## Usage

To use this recipe in your OpenRewrite project:

### 1. Add to `rewrite.yml`

```yaml
rewrite:
  activeRecipes:
    - com.rewrite.ChangeConstantsReference
```
