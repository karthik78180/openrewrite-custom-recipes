# Records (JDK 14 Preview → JDK 16 Final)

## Overview

Records are a special kind of class in Java that act as transparent carriers for immutable data. They were introduced as a preview feature in JDK 14 and became final in JDK 16.

## What Problem Does It Solve?

Before records, creating simple data carrier classes required a lot of boilerplate code:

```java
// Pre-JDK 16: Traditional approach
public final class Person {
    private final String name;
    private final int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() { return name; }
    public int getAge() { return age; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return age == person.age && Objects.equals(name, person.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + '}';
    }
}
```

## The Records Solution

```java
// JDK 16+: Using records
public record Person(String name, int age) {}
```

That's it! The compiler automatically generates:
- Private final fields for each component
- A canonical constructor
- Accessor methods (not getters, but direct field name methods)
- `equals()`, `hashCode()`, and `toString()` methods

## Key Features

### 1. Automatic Component Accessors

```java
public record Person(String name, int age) {}

Person person = new Person("Alice", 30);
String name = person.name();  // Note: name(), not getName()
int age = person.age();        // Note: age(), not getAge()
```

### 2. Compact Constructor

You can add validation without writing the full constructor:

```java
public record Person(String name, int age) {
    public Person {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
    }
}
```

### 3. Custom Constructors

You can add alternative constructors:

```java
public record Person(String name, int age) {
    // Compact constructor for validation
    public Person {
        Objects.requireNonNull(name);
        if (age < 0) throw new IllegalArgumentException("Age must be positive");
    }

    // Alternative constructor
    public Person(String name) {
        this(name, 0);
    }
}
```

### 4. Additional Methods

Records can have instance methods and static methods:

```java
public record Person(String name, int age) {
    public boolean isAdult() {
        return age >= 18;
    }

    public static Person child(String name) {
        return new Person(name, 0);
    }
}
```

## Important Restrictions

1. **Records are implicitly final** - Cannot be extended
2. **Records cannot extend other classes** - But can implement interfaces
3. **Component fields are implicitly final** - Immutable by design
4. **Cannot declare instance fields** - Only static fields allowed
5. **Cannot be abstract**

## Use Cases in Backend Development

### 1. DTOs (Data Transfer Objects)

```java
// API Request/Response objects
public record CreateUserRequest(String username, String email, String password) {
    public CreateUserRequest {
        Objects.requireNonNull(username, "username is required");
        Objects.requireNonNull(email, "email is required");
        Objects.requireNonNull(password, "password is required");
    }
}

public record UserResponse(Long id, String username, String email, Instant createdAt) {}
```

### 2. Database Query Results

```java
public record UserStats(String username, long orderCount, BigDecimal totalSpent) {}

// In repository
public List<UserStats> getUserStatistics() {
    return jdbcTemplate.query(
        "SELECT username, COUNT(*) as order_count, SUM(amount) as total_spent FROM orders GROUP BY username",
        (rs, rowNum) -> new UserStats(
            rs.getString("username"),
            rs.getLong("order_count"),
            rs.getBigDecimal("total_spent")
        )
    );
}
```

### 3. Configuration Objects

```java
public record DatabaseConfig(
    String host,
    int port,
    String database,
    String username,
    String password,
    int maxConnections
) {
    public String jdbcUrl() {
        return "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
    }
}
```

### 4. Domain Events

```java
public record OrderPlacedEvent(
    Long orderId,
    Long customerId,
    BigDecimal totalAmount,
    Instant timestamp
) {}

public record PaymentProcessedEvent(
    Long paymentId,
    Long orderId,
    PaymentStatus status,
    Instant timestamp
) {}
```

### 5. Multi-Value Returns

```java
public record ValidationResult(boolean isValid, List<String> errors) {
    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(String... errors) {
        return new ValidationResult(false, List.of(errors));
    }
}

public ValidationResult validateOrder(Order order) {
    List<String> errors = new ArrayList<>();
    if (order.items().isEmpty()) {
        errors.add("Order must contain at least one item");
    }
    if (order.totalAmount().compareTo(BigDecimal.ZERO) <= 0) {
        errors.add("Order total must be positive");
    }
    return errors.isEmpty()
        ? ValidationResult.valid()
        : new ValidationResult(false, errors);
}
```

## Working with Frameworks

### Spring Framework

Records work seamlessly with Spring:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        // Jackson automatically deserializes to record
        User user = userService.create(request);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getEmail()));
    }
}
```

### JPA/Hibernate

While entities should not be records (they need to be mutable), records are perfect for projections:

```java
// Entity (not a record)
@Entity
public class User {
    @Id
    @GeneratedValue
    private Long id;
    private String username;
    private String email;
    // ... getters, setters
}

// Projection (record)
public record UserProjection(Long id, String username) {}

// Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT new com.example.UserProjection(u.id, u.username) FROM User u")
    List<UserProjection> findAllProjections();
}
```

## Migration Strategy

1. **Identify Candidates**: Look for immutable POJOs, DTOs, value objects
2. **Convert Incrementally**: Start with simple data carriers
3. **Update Tests**: Ensure tests work with record accessors (`name()` vs `getName()`)
4. **Framework Compatibility**: Verify your frameworks support records (most modern versions do)

## Performance Considerations

- **Memory**: Records are more memory-efficient than equivalent classes
- **Serialization**: Records serialize efficiently with modern libraries (Jackson 2.12+, Gson 2.8.9+)
- **Equals/HashCode**: Compiler-generated implementations are optimized

## Common Pitfalls

1. **Using records for JPA entities** - Don't! Entities need to be mutable
2. **Expecting getters** - Records use `field()` not `getField()`
3. **Trying to modify fields** - Records are immutable; create new instances instead
4. **Over-engineering** - Keep records simple; if you need complex behavior, use a class

## Summary

Records are a game-changer for Java backend development, reducing boilerplate and making code more readable and maintainable. They're perfect for DTOs, value objects, and any immutable data carrier.

**Migration Priority**: HIGH - Safe and provides immediate value with reduced boilerplate.
