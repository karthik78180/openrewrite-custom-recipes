# Pattern Matching for instanceof (JDK 14 Preview → JDK 16 Final)

## Overview

Pattern matching for `instanceof` eliminates the need for explicit casting after type checks. This feature was introduced as a preview in JDK 14 and became final in JDK 16.

## What Problem Does It Solve?

The traditional `instanceof` pattern required redundant code:

```java
// Pre-JDK 16: Traditional approach
if (obj instanceof String) {
    String str = (String) obj;  // Explicit cast after check
    System.out.println(str.length());
}
```

This pattern had several issues:
1. Redundant code - type appears three times
2. Potential for errors - could cast to wrong type
3. Verbose - especially with multiple checks

## The Pattern Matching Solution

```java
// JDK 16+: Pattern matching
if (obj instanceof String str) {
    System.out.println(str.length());  // 'str' is automatically cast
}
```

The pattern variable `str` is:
- Automatically declared
- Automatically cast to the target type
- Scoped to where it's definitely assigned

## Scope and Flow Typing

The pattern variable is only in scope where it's guaranteed to be assigned:

```java
public void example(Object obj) {
    // Pattern variable scoped to if block
    if (obj instanceof String str) {
        System.out.println(str.length());  // ✓ str is in scope
    }
    // System.out.println(str.length());   // ✗ str is not in scope here

    // Works with && - str is in scope for the second condition
    if (obj instanceof String str && str.length() > 5) {
        System.out.println("Long string: " + str);
    }

    // Doesn't work with || - str might not be assigned
    // if (obj instanceof String str || str.length() > 5) {  // ✗ Compile error
    //     System.out.println(str);
    // }

    // Works with negation and else
    if (!(obj instanceof String str)) {
        System.out.println("Not a string");
    } else {
        System.out.println(str.toUpperCase());  // ✓ str is in scope
    }
}
```

## Use Cases in Backend Development

### 1. Polymorphic Method Handling

```java
public class PaymentProcessor {

    public Receipt processPayment(Payment payment) {
        if (payment instanceof CreditCardPayment cc) {
            return processCreditCard(cc.cardNumber(), cc.cvv(), cc.amount());
        } else if (payment instanceof BankTransferPayment bt) {
            return processBankTransfer(bt.accountNumber(), bt.routingNumber(), bt.amount());
        } else if (payment instanceof PayPalPayment pp) {
            return processPayPal(pp.email(), pp.amount());
        } else {
            throw new UnsupportedPaymentMethodException(payment.getClass());
        }
    }
}
```

### 2. Exception Handling

```java
public ApiResponse<User> handleUserOperation() {
    try {
        User user = userService.performOperation();
        return new Success<>(user);
    } catch (Exception e) {
        if (e instanceof UserNotFoundException unf) {
            return new ClientError<>("User not found: " + unf.getUserId(), 404);
        } else if (e instanceof ValidationException ve) {
            return new ClientError<>("Validation failed: " + ve.getErrors(), 400);
        } else if (e instanceof DatabaseException dbe) {
            logger.error("Database error", dbe);
            return new ServerError<>("Database unavailable", 503);
        } else {
            logger.error("Unexpected error", e);
            return new ServerError<>("Internal server error", 500);
        }
    }
}
```

### 3. Message/Event Processing

```java
public class EventHandler {

    public void handleEvent(DomainEvent event) {
        if (event instanceof OrderCreatedEvent oce) {
            logger.info("Order {} created for customer {}", oce.orderId(), oce.customerId());
            inventoryService.reserveItems(oce.items());
            emailService.sendOrderConfirmation(oce);
        } else if (event instanceof OrderCancelledEvent oce) {
            logger.info("Order {} cancelled: {}", oce.orderId(), oce.reason());
            inventoryService.releaseItems(oce.orderId());
            paymentService.refund(oce.orderId());
        } else if (event instanceof PaymentReceivedEvent pre) {
            logger.info("Payment {} received for order {}", pre.paymentId(), pre.orderId());
            orderService.markAsPaid(pre.orderId());
            shippingService.scheduleShipment(pre.orderId());
        }
    }
}
```

### 4. Request/Response Processing

```java
@RestController
public class ApiController {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        if (e instanceof EntityNotFoundException enf && enf.getEntityType().equals("User")) {
            return ResponseEntity.status(404).body(
                new ErrorResponse("User not found", List.of(enf.getMessage()))
            );
        } else if (e instanceof ValidationException ve) {
            return ResponseEntity.status(400).body(
                new ErrorResponse("Validation failed", ve.getErrors())
            );
        } else if (e instanceof AuthenticationException ae) {
            return ResponseEntity.status(401).body(
                new ErrorResponse("Authentication failed", List.of(ae.getMessage()))
            );
        } else if (e instanceof AccessDeniedException ade) {
            return ResponseEntity.status(403).body(
                new ErrorResponse("Access denied", List.of(ade.getMessage()))
            );
        } else {
            logger.error("Unhandled exception", e);
            return ResponseEntity.status(500).body(
                new ErrorResponse("Internal server error", List.of())
            );
        }
    }
}
```

### 5. Data Validation and Transformation

```java
public class DataConverter {

    public String convertToString(Object value) {
        if (value instanceof String str) {
            return str;
        } else if (value instanceof Integer num) {
            return String.valueOf(num);
        } else if (value instanceof Double dbl) {
            return String.format("%.2f", dbl);
        } else if (value instanceof Boolean bool) {
            return bool ? "true" : "false";
        } else if (value instanceof LocalDate date) {
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        } else if (value instanceof Collection<?> coll) {
            return coll.stream()
                .map(this::convertToString)
                .collect(Collectors.joining(", "));
        } else if (value == null) {
            return "";
        } else {
            return value.toString();
        }
    }
}
```

### 6. Repository Layer

```java
public class GenericRepository {

    public <T> List<T> findByAttribute(String attribute, Object value, Class<T> entityClass) {
        String sql = "SELECT * FROM " + getTableName(entityClass) + " WHERE " + attribute + " = ?";

        Object paramValue;
        if (value instanceof String str) {
            paramValue = str.trim();
        } else if (value instanceof LocalDate date) {
            paramValue = Date.valueOf(date);
        } else if (value instanceof LocalDateTime dateTime) {
            paramValue = Timestamp.valueOf(dateTime);
        } else if (value instanceof Enum<?> enumValue) {
            paramValue = enumValue.name();
        } else {
            paramValue = value;
        }

        return jdbcTemplate.query(sql, rowMapper(entityClass), paramValue);
    }
}
```

### 7. Builder Pattern with Validation

```java
public class QueryBuilder {

    private final List<String> conditions = new ArrayList<>();
    private final List<Object> parameters = new ArrayList<>();

    public QueryBuilder addCondition(String field, Object value) {
        if (value instanceof String str && !str.isBlank()) {
            conditions.add(field + " = ?");
            parameters.add(str.trim());
        } else if (value instanceof Number num && num.doubleValue() > 0) {
            conditions.add(field + " = ?");
            parameters.add(num);
        } else if (value instanceof Boolean bool) {
            conditions.add(field + " = ?");
            parameters.add(bool);
        } else if (value instanceof Collection<?> coll && !coll.isEmpty()) {
            String placeholders = coll.stream().map(v -> "?").collect(Collectors.joining(", "));
            conditions.add(field + " IN (" + placeholders + ")");
            parameters.addAll(coll);
        }
        return this;
    }

    public PreparedQuery build() {
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new PreparedQuery("SELECT * FROM table" + where, parameters);
    }
}
```

## Combining with Logical Operators

### AND Operation

```java
// Pattern variable is in scope after &&
if (obj instanceof String str && str.length() > 10) {
    System.out.println("Long string: " + str.toUpperCase());
}

// More complex example
if (payment instanceof CreditCardPayment cc &&
    cc.amount().compareTo(LARGE_TRANSACTION_THRESHOLD) > 0 &&
    !cc.cardNumber().startsWith("4111")) {
    fraudDetectionService.verify(cc);
}
```

### Negation

```java
// Pattern variable is in scope in else block
if (!(obj instanceof String str)) {
    throw new IllegalArgumentException("Expected string");
} else {
    return str.toUpperCase();
}

// Early return pattern
public void process(Object obj) {
    if (!(obj instanceof ProcessableData data)) {
        return;
    }
    // 'data' is in scope for rest of method
    data.process();
    data.save();
}
```

## Real-World Example: Multi-Format Parser

```java
public class DataParser {

    public ParsedData parse(Object input) {
        if (input instanceof String json && json.trim().startsWith("{")) {
            return parseJson(json);
        } else if (input instanceof String xml && xml.trim().startsWith("<")) {
            return parseXml(xml);
        } else if (input instanceof byte[] bytes && bytes.length > 0) {
            return parseBinary(bytes);
        } else if (input instanceof InputStream stream) {
            try {
                return parse(stream.readAllBytes());
            } catch (IOException e) {
                throw new ParseException("Failed to read input stream", e);
            }
        } else if (input instanceof File file && file.exists()) {
            try {
                return parse(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                throw new ParseException("Failed to read file: " + file.getName(), e);
            }
        } else {
            throw new ParseException("Unsupported input type: " +
                (input == null ? "null" : input.getClass().getName()));
        }
    }
}
```

## Benefits Over Traditional instanceof

| Aspect | Traditional | Pattern Matching |
|--------|------------|------------------|
| Lines of code | 3 lines | 1 line |
| Type safety | Manual cast (error-prone) | Automatic (safe) |
| Readability | Verbose | Concise |
| Maintenance | Easy to introduce bugs | Hard to make mistakes |
| Refactoring | Need to update cast | Automatic |

## Common Patterns

### Early Return Pattern

```java
public void processUser(Object obj) {
    if (!(obj instanceof User user)) {
        logger.warn("Expected User but got: {}", obj.getClass());
        return;
    }

    // Rest of method works with 'user'
    validateUser(user);
    saveUser(user);
    notifyUser(user);
}
```

### Guard Clauses

```java
public BigDecimal calculateDiscount(Object customer, BigDecimal amount) {
    if (!(customer instanceof PremiumCustomer premium)) {
        return BigDecimal.ZERO;
    }

    if (premium.membershipLevel() < 3) {
        return amount.multiply(new BigDecimal("0.05"));
    }

    return amount.multiply(new BigDecimal("0.10"));
}
```

## Migration Strategy

1. **Search and Replace**: Look for traditional instanceof + cast patterns
2. **IDE Support**: Most modern IDEs can automatically convert these
3. **Test Thoroughly**: Ensure pattern variable scoping doesn't break logic
4. **Code Review**: Verify conditions with && and || work as expected

## Performance

- **No runtime overhead**: Same performance as manual cast
- **Compiler optimization**: Better than or equal to traditional approach
- **Bytecode**: Nearly identical to manual cast pattern

## Common Pitfalls

1. **OR condition**: Pattern variable NOT available after `||`
   ```java
   // ✗ Won't compile
   // if (obj instanceof String str || str.isEmpty()) { }

   // ✓ Correct
   if (obj instanceof String str && str.isEmpty()) { }
   ```

2. **Variable shadowing**: Be careful with variable names
   ```java
   String str = "outer";
   if (obj instanceof String str) {  // New 'str' shadows outer
       // This 'str' is the pattern variable
   }
   ```

3. **Scope confusion**: Pattern variable only in scope where guaranteed
   ```java
   if (obj instanceof String str) {
       // str in scope
   }
   // str NOT in scope here
   ```

## Summary

Pattern matching for `instanceof` eliminates boilerplate code, improves readability, and reduces bugs. It's particularly valuable in polymorphic method handling, exception processing, and data validation scenarios common in backend development.

**Migration Priority**: HIGH - Safe, immediate improvement with no downsides. IDE can automate conversion.
