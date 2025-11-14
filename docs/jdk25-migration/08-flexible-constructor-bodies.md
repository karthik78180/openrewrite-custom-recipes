# Flexible Constructor Bodies (JDK 25 Final)

## Overview

Flexible constructor bodies allow statements to appear before an explicit constructor invocation (`super()` or `this()`). This feature became final in JDK 25 and removes a long-standing Java restriction.

## What Problem Does It Solve?

Before JDK 25, you couldn't execute any statements before calling `super()` or `this()`:

```java
// Pre-JDK 25: Workaround required
public class User {
    private final String username;
    private final String normalizedEmail;

    public User(String username, String email) {
        // ✗ Can't do this - must call super() first
        // String normalized = email.toLowerCase().trim();
        // super();  // Implicit super() call

        // Workaround: Use helper method
        this(username, normalizeEmail(email));
    }

    private User(String username, String normalizedEmail) {
        this.username = username;
        this.normalizedEmail = normalizedEmail;
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        return email.toLowerCase().trim();
    }
}
```

## The Flexible Constructor Bodies Solution

```java
// JDK 25+: Statements before super/this allowed
public class User {
    private final String username;
    private final String normalizedEmail;

    public User(String username, String email) {
        // ✓ Can now do this!
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        }
        String normalized = email.toLowerCase().trim();

        // Now call super
        super();

        // Initialize fields
        this.username = username;
        this.normalizedEmail = normalized;
    }
}
```

## Key Features

### 1. Validation Before Construction

```java
public class Order {
    private final List<OrderItem> items;
    private final BigDecimal total;

    public Order(List<OrderItem> items) {
        // Validate before calling super
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        // Calculate total before super
        BigDecimal calculatedTotal = items.stream()
            .map(OrderItem::price)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (calculatedTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }

        super();  // Now call super

        this.items = new ArrayList<>(items);  // Defensive copy
        this.total = calculatedTotal;
    }
}
```

### 2. Data Normalization

```java
public class EmailAddress {
    private final String local;
    private final String domain;

    public EmailAddress(String email) {
        // Normalize before construction
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        String normalized = email.toLowerCase().trim();
        String[] parts = normalized.split("@");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Now call super
        super();

        this.local = parts[0];
        this.domain = parts[1];
    }
}
```

### 3. Resource Initialization

```java
public class DatabaseConnection extends Connection {
    private final String jdbcUrl;
    private final Properties props;

    public DatabaseConnection(String host, int port, String database, String user, String password) {
        // Prepare connection string and properties before super
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);

        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("ssl", "true");
        properties.setProperty("connectTimeout", "30");

        // Call parent constructor with prepared values
        super(url, properties);

        this.jdbcUrl = url;
        this.props = properties;
    }
}
```

## Use Cases in Backend Development

### 1. Complex Validation in Domain Objects

```java
public class CreditCard extends PaymentMethod {
    private final String maskedNumber;
    private final CardType type;
    private final YearMonth expiry;

    public CreditCard(String number, String cvv, YearMonth expiry) {
        // Validate card number
        if (number == null || !number.matches("\\d{13,19}")) {
            throw new IllegalArgumentException("Invalid card number");
        }

        // Validate CVV
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new IllegalArgumentException("Invalid CVV");
        }

        // Validate expiry
        if (expiry == null || expiry.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("Card has expired");
        }

        // Determine card type
        CardType cardType = determineCardType(number);

        // Mask the number
        String masked = maskCardNumber(number);

        super();  // Call PaymentMethod constructor

        this.maskedNumber = masked;
        this.type = cardType;
        this.expiry = expiry;
    }

    private CardType determineCardType(String number) {
        if (number.startsWith("4")) return CardType.VISA;
        if (number.startsWith("5")) return CardType.MASTERCARD;
        if (number.startsWith("3")) return CardType.AMEX;
        return CardType.UNKNOWN;
    }

    private String maskCardNumber(String number) {
        return "**** **** **** " + number.substring(number.length() - 4);
    }
}
```

### 2. DTO with Derived Fields

```java
public class OrderResponse extends BaseResponse {
    private final Long orderId;
    private final String orderNumber;
    private final OrderStatus status;
    private final String statusDisplay;
    private final BigDecimal subtotal;
    private final BigDecimal tax;
    private final BigDecimal total;

    public OrderResponse(Order order) {
        // Calculate derived values before super
        BigDecimal subtotal = order.getSubtotal();
        BigDecimal taxRate = order.getTaxRate();
        BigDecimal tax = subtotal.multiply(taxRate);
        BigDecimal total = subtotal.add(tax);

        // Format status for display
        String statusDisplay = formatStatus(order.getStatus());

        super(true, null);  // Call BaseResponse constructor

        this.orderId = order.getId();
        this.orderNumber = order.getOrderNumber();
        this.status = order.getStatus();
        this.statusDisplay = statusDisplay;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
    }

    private String formatStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Pending Payment";
            case PROCESSING -> "Processing";
            case SHIPPED -> "Shipped";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Cancelled";
        };
    }
}
```

### 3. Configuration Objects with Defaults

```java
public class ApplicationConfig extends BaseConfig {
    private final int port;
    private final String host;
    private final int maxConnections;
    private final Duration timeout;

    public ApplicationConfig(Map<String, String> properties) {
        // Parse and validate properties before super
        String portStr = properties.get("server.port");
        int port = portStr != null ? Integer.parseInt(portStr) : 8080;

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }

        String host = properties.getOrDefault("server.host", "0.0.0.0");

        String maxConnStr = properties.get("server.max-connections");
        int maxConnections = maxConnStr != null ? Integer.parseInt(maxConnStr) : 100;

        String timeoutStr = properties.get("server.timeout");
        Duration timeout = timeoutStr != null ?
            Duration.parse(timeoutStr) : Duration.ofSeconds(30);

        super(properties);  // Pass to parent

        this.port = port;
        this.host = host;
        this.maxConnections = maxConnections;
        this.timeout = timeout;
    }
}
```

### 4. Entity with Audit Fields

```java
@Entity
public class AuditableEntity extends BaseEntity {
    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private String modifiedBy;

    @Column
    private Instant modifiedAt;

    protected AuditableEntity() {
        // Get current user and timestamp before super
        String currentUser = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();

        Instant now = Instant.now();

        super();  // Call BaseEntity constructor

        this.createdBy = currentUser;
        this.createdAt = now;
        this.modifiedBy = currentUser;
        this.modifiedAt = now;
    }
}
```

### 5. Builder Pattern with Validation

```java
public class User extends BaseUser {
    private final String username;
    private final String email;
    private final Set<Role> roles;

    private User(Builder builder) {
        // Validate required fields before super
        if (builder.username == null || builder.username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (builder.email == null || !builder.email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }

        // Normalize data
        String normalizedUsername = builder.username.toLowerCase().trim();
        String normalizedEmail = builder.email.toLowerCase().trim();

        // Ensure non-null collections
        Set<Role> roles = builder.roles != null ?
            Set.copyOf(builder.roles) : Set.of(Role.USER);

        super();  // Call parent constructor

        this.username = normalizedUsername;
        this.email = normalizedEmail;
        this.roles = roles;
    }

    public static class Builder {
        private String username;
        private String email;
        private Set<Role> roles;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```

### 6. Event Objects with Metadata

```java
public class OrderEvent extends DomainEvent {
    private final Long orderId;
    private final EventType type;
    private final Instant timestamp;
    private final String correlationId;

    public OrderEvent(Long orderId, EventType type) {
        // Generate metadata before super
        Instant timestamp = Instant.now();

        // Get or generate correlation ID
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        super("order." + type.name().toLowerCase(), correlationId);

        this.orderId = orderId;
        this.type = type;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }
}
```

### 7. Repository Query Objects

```java
public class UserQuery extends BaseQuery {
    private final String username;
    private final String email;
    private final LocalDate createdAfter;
    private final Set<Role> roles;
    private final int offset;
    private final int limit;

    public UserQuery(UserQueryParams params) {
        // Validate and normalize parameters
        if (params.limit() <= 0 || params.limit() > 1000) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000");
        }

        if (params.offset() < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }

        // Normalize strings
        String username = params.username() != null ?
            params.username().toLowerCase().trim() : null;

        String email = params.email() != null ?
            params.email().toLowerCase().trim() : null;

        super();  // Call parent

        this.username = username;
        this.email = email;
        this.createdAfter = params.createdAfter();
        this.roles = params.roles() != null ? Set.copyOf(params.roles()) : Set.of();
        this.offset = params.offset();
        this.limit = params.limit();
    }
}
```

## Restrictions

You still cannot:
- **Reference `this`** before `super()`/`this()`
- **Call instance methods** before `super()`/`this()`
- **Access instance fields** before `super()`/`this()`

```java
public class Example {
    private int value;

    public Example(int value) {
        // ✓ OK - local variable
        int doubled = value * 2;

        // ✓ OK - static method
        String formatted = String.format("Value: %d", value);

        // ✗ NOT OK - references 'this'
        // this.value = doubled;

        // ✗ NOT OK - instance method call
        // this.someMethod();

        super();  // Must call super before accessing instance members

        this.value = doubled;  // ✓ OK after super
    }
}
```

## Benefits

1. **Eliminates Helper Methods**: No need for static helper methods just to process constructor arguments
2. **Cleaner Validation**: Validate directly in constructor instead of before construction
3. **Better Readability**: Constructor logic flows naturally
4. **Type Safety**: Can perform type conversions before field assignment
5. **Reduced Boilerplate**: Less auxiliary code needed

## Migration Strategy

1. **Identify Patterns**: Look for static helper methods used only in constructors
2. **Inline Validation**: Move validation from factory methods to constructors
3. **Simplify**: Remove unnecessary private constructors and helper methods
4. **Test**: Ensure constructor behavior remains correct

## Common Patterns

### Before JDK 25

```java
public class Product {
    private final String sku;
    private final BigDecimal price;

    public Product(String sku, String priceStr) {
        this(sku, parsePrice(priceStr));  // Delegate to private constructor
    }

    private Product(String sku, BigDecimal price) {
        this.sku = sku;
        this.price = price;
    }

    private static BigDecimal parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isBlank()) {
            throw new IllegalArgumentException("Price required");
        }
        return new BigDecimal(priceStr);
    }
}
```

### After JDK 25

```java
public class Product {
    private final String sku;
    private final BigDecimal price;

    public Product(String sku, String priceStr) {
        // Parse and validate directly
        if (priceStr == null || priceStr.isBlank()) {
            throw new IllegalArgumentException("Price required");
        }
        BigDecimal price = new BigDecimal(priceStr);

        super();

        this.sku = sku;
        this.price = price;
    }
}
```

## Performance

- **No runtime overhead**: Purely a compile-time feature
- **Same bytecode structure**: Similar to previous workarounds
- **JIT optimization**: Same optimization potential as before

## Summary

Flexible constructor bodies remove an artificial restriction, making Java code more natural and reducing boilerplate. This is especially useful for domain objects with complex validation and derived fields common in backend applications.

**Migration Priority**: LOW-MEDIUM - Nice improvement for new code, but migration of existing code provides limited benefit. Use opportunistically.
