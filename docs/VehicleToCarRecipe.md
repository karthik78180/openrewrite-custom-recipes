# VehicleToCarRecipe

This OpenRewrite recipe updates all Java classes that extend `Vehicle` (with or without generics) to extend `Car` instead.

---

## ✅ Features

- Replaces `extends Vehicle` with `extends Car`
- Supports `Vehicle<T>` → `Car<T>` (preserves generic type parameters)
- Handles both raw and parameterized superclasses
- Adjusts import statements accordingly
- Uses OpenRewrite 8.23.1-compatible APIs

---

## 📌 Use Case

Suppose you're deprecating the `Vehicle` class in favor of `Car`, and you want to migrate all classes across your codebase automatically. This recipe does that with AST-safe transformations.

---

## ⚙️ How It Works

1. Traverses all class declarations using `JavaIsoVisitor`.
2. Identifies if the class directly extends `Vehicle` or `Vehicle<T>`.
3. Replaces the superclass with `Car` (preserving type parameters when present).
4. Adds/removes imports as necessary.

---

## 🧩 Usage

To use this recipe in a rewrite project:

1. Add dependency to your OpenRewrite project
2. Apply recipe in rewrite configuration:
```yaml
recipe:
  - com.rewrite.VehicleToCarRecipe
```
