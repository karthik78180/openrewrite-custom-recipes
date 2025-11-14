# Virtual Threads (JDK 19 Preview → JDK 21 Final)

## Overview

Virtual threads are lightweight threads that dramatically reduce the effort of writing, maintaining, and observing high-throughput concurrent applications. They were introduced as a preview in JDK 19 and became final in JDK 21.

## What Problem Does It Solve?

Traditional platform threads are expensive:
- **Heavy**: Each platform thread consumes ~1MB of memory
- **Limited**: OS-level limitation (~few thousand threads max)
- **Blocking**: Thread-per-request model wastes resources when blocked on I/O

```java
// Pre-JDK 21: Traditional thread pool
ExecutorService executor = Executors.newFixedThreadPool(200);  // Limited to 200 threads

for (int i = 0; i < 10_000; i++) {
    executor.submit(() -> {
        // If this blocks on I/O, thread is wasted
        String result = httpClient.get("https://api.example.com/data");
        processResult(result);
    });
}
// Can only handle 200 concurrent requests at a time
```

## The Virtual Threads Solution

Virtual threads are:
- **Cheap**: Can create millions of them
- **Lightweight**: ~1KB memory footprint
- **Managed by JVM**: Not OS threads
- **Cooperative**: Automatically yield when blocked

```java
// JDK 21+: Virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
        executor.submit(() -> {
            // Blocks don't waste resources - virtual thread parks
            String result = httpClient.get("https://api.example.com/data");
            processResult(result);
        });
    }
    // Can handle millions of concurrent requests!
}
```

## Creating Virtual Threads

### 1. Thread.ofVirtual() Builder

```java
// Single virtual thread
Thread vThread = Thread.ofVirtual()
    .name("my-virtual-thread")
    .start(() -> {
        System.out.println("Running in virtual thread: " + Thread.currentThread());
    });

vThread.join();
```

### 2. Virtual Thread Per Task Executor

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1_000_000; i++) {
        int taskId = i;
        executor.submit(() -> {
            performTask(taskId);
        });
    }
} // Executor waits for all tasks to complete
```

### 3. Thread Factory

```java
ThreadFactory factory = Thread.ofVirtual().factory();

Thread vt1 = factory.newThread(() -> System.out.println("Task 1"));
Thread vt2 = factory.newThread(() -> System.out.println("Task 2"));

vt1.start();
vt2.start();
```

### 4. Direct Creation

```java
Thread vt = Thread.startVirtualThread(() -> {
    System.out.println("Quick virtual thread");
});

vt.join();
```

## Use Cases in Backend Development

### 1. HTTP Request Handling (Spring Boot)

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }
}

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;

    @PostMapping
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        // Each request runs in a virtual thread
        // Blocking calls don't waste resources

        // Call 1: Check inventory (blocks)
        boolean available = inventoryService.checkAvailability(request.items());

        if (!available) {
            throw new InsufficientInventoryException();
        }

        // Call 2: Process payment (blocks)
        PaymentResult payment = paymentService.processPayment(request.paymentMethod(), request.total());

        if (!payment.isSuccess()) {
            throw new PaymentFailedException();
        }

        // Call 3: Create order (blocks)
        Order order = orderService.createOrder(request, payment.transactionId());

        return toResponse(order);
    }
}
```

### 2. Parallel I/O Operations

```java
public class DataAggregationService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AggregatedData fetchDataFromMultipleSources(String userId) throws InterruptedException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit parallel HTTP requests
            Future<UserProfile> profileFuture = executor.submit(() ->
                fetchUserProfile(userId)
            );

            Future<List<Order>> ordersFuture = executor.submit(() ->
                fetchUserOrders(userId)
            );

            Future<List<Payment>> paymentsFuture = executor.submit(() ->
                fetchUserPayments(userId)
            );

            Future<List<Address>> addressesFuture = executor.submit(() ->
                fetchUserAddresses(userId)
            );

            Future<Preferences> preferencesFuture = executor.submit(() ->
                fetchUserPreferences(userId)
            );

            // Wait for all to complete and aggregate
            return new AggregatedData(
                profileFuture.get(),
                ordersFuture.get(),
                paymentsFuture.get(),
                addressesFuture.get(),
                preferencesFuture.get()
            );
        } catch (ExecutionException e) {
            throw new DataFetchException("Failed to aggregate data", e);
        }
    }

    private UserProfile fetchUserProfile(String userId) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://profile-service/users/" + userId))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), UserProfile.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch profile", e);
        }
    }

    // Similar methods for other services...
}
```

### 3. Database Connection Pooling

Virtual threads change how we think about connection pools:

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public HikariDataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
        config.setUsername("user");
        config.setPassword("password");

        // With virtual threads, we can afford larger pools
        config.setMaximumPoolSize(500);  // Was typically 10-50 with platform threads
        config.setMinimumIdle(100);

        return new HikariDataSource(config);
    }
}

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<Order> findOrdersByUserId(Long userId) {
        // This blocks on database I/O
        // With virtual threads, thread is parked (not wasted)
        return jdbcTemplate.query(
            "SELECT * FROM orders WHERE user_id = ?",
            orderRowMapper,
            userId
        );
    }

    public void processLargeBatch(List<Order> orders) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Order order : orders) {
                executor.submit(() -> {
                    // Each order processed in its own virtual thread
                    // Thousands of concurrent database operations possible
                    updateOrderStatus(order);
                    createInvoice(order);
                    sendNotification(order);
                });
            }
        }
    }
}
```

### 4. Message Queue Processing

```java
@Service
public class MessageProcessor {

    @JmsListener(destination = "order.events", concurrency = "1")
    public void processOrderEvents(Message message) throws JMSException {
        String eventJson = ((TextMessage) message).getText();
        OrderEvent event = objectMapper.readValue(eventJson, OrderEvent.class);

        // Process each message in a virtual thread
        Thread.startVirtualThread(() -> {
            handleOrderEvent(event);
        });
    }

    private void handleOrderEvent(OrderEvent event) {
        switch (event) {
            case OrderCreated e -> {
                // Blocking calls are fine with virtual threads
                inventoryService.reserve(e.orderId(), e.items());
                emailService.sendOrderConfirmation(e.customerId(), e.orderId());
                analyticsService.trackOrderCreated(e);
            }
            case OrderPaid e -> {
                shippingService.scheduleShipment(e.orderId());
                loyaltyService.awardPoints(e.customerId(), e.amount());
            }
            case OrderShipped e -> {
                emailService.sendShipmentNotification(e.customerId(), e.trackingNumber());
            }
            // ... other events
        }
    }
}
```

### 5. Batch Processing

```java
public class BatchProcessor {

    public void processDailyReports() {
        List<Long> userIds = userRepository.findAllActiveUserIds();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Long userId : userIds) {
                executor.submit(() -> {
                    try {
                        // Generate report (CPU + I/O)
                        Report report = reportGenerator.generate(userId);

                        // Store to database (I/O)
                        reportRepository.save(report);

                        // Upload to S3 (I/O)
                        s3Client.uploadReport(report);

                        // Send email (I/O)
                        emailService.sendReport(userId, report);

                        logger.info("Processed report for user {}", userId);
                    } catch (Exception e) {
                        logger.error("Failed to process report for user {}", userId, e);
                    }
                });
            }
        }

        logger.info("All {} reports processed", userIds.size());
    }
}
```

### 6. WebSocket Connections

```java
@ServerEndpoint("/ws/notifications")
public class NotificationWebSocket {

    private static final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);

        // Use virtual thread to handle this connection
        Thread.startVirtualThread(() -> {
            try {
                // Send periodic updates
                while (session.isOpen()) {
                    String notification = notificationService.getNextNotification(session.getId());
                    if (notification != null) {
                        session.getBasicRemote().sendText(notification);
                    }
                    Thread.sleep(1000);  // Virtual thread parks here
                }
            } catch (Exception e) {
                logger.error("Error in notification websocket", e);
            }
        });
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Handle each message in a virtual thread
        Thread.startVirtualThread(() -> {
            processMessage(message, session);
        });
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
    }

    public static void broadcastNotification(String notification) {
        // Broadcast to all sessions in parallel using virtual threads
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Session session : sessions) {
                executor.submit(() -> {
                    try {
                        session.getBasicRemote().sendText(notification);
                    } catch (IOException e) {
                        logger.error("Failed to send notification", e);
                    }
                });
            }
        }
    }
}
```

### 7. Structured Concurrency (Preview)

```java
public class StructuredTaskService {

    public OrderValidationResult validateOrder(Order order) throws InterruptedException {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // Submit parallel validation tasks
            Subtask<Boolean> inventoryCheck = scope.fork(() ->
                inventoryService.isAvailable(order.items())
            );

            Subtask<Boolean> paymentCheck = scope.fork(() ->
                paymentService.canProcess(order.paymentMethod(), order.total())
            );

            Subtask<Boolean> fraudCheck = scope.fork(() ->
                fraudService.checkOrder(order)
            );

            // Wait for all to complete or fail fast
            scope.join();
            scope.throwIfFailed();

            // All succeeded, get results
            return new OrderValidationResult(
                inventoryCheck.get(),
                paymentCheck.get(),
                fraudCheck.get()
            );
        } catch (ExecutionException e) {
            throw new ValidationException("Order validation failed", e);
        }
    }
}
```

## Best Practices

### 1. Don't Pool Virtual Threads

```java
// ✗ BAD - Don't pool virtual threads
ExecutorService pool = Executors.newFixedThreadPool(1000, Thread.ofVirtual().factory());

// ✓ GOOD - Create new virtual threads per task
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

### 2. Use Try-With-Resources for Executors

```java
// ✓ GOOD - Ensures shutdown
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // submit tasks
}

// ✗ BAD - Might forget to shutdown
var executor = Executors.newVirtualThreadPerTaskExecutor();
// submit tasks
executor.shutdown();
```

### 3. Avoid ThreadLocal with Virtual Threads

```java
// ✗ BAD - Expensive with millions of virtual threads
private static final ThreadLocal<DateFormatter> formatter = ThreadLocal.withInitial(DateFormatter::new);

// ✓ GOOD - Use scoped values (preview) or regular local variables
public void processDate(String dateStr) {
    DateFormatter formatter = new DateFormatter();  // Cheap to create
    formatter.parse(dateStr);
}
```

### 4. Pinning Detection

Monitor for thread pinning (when virtual thread can't unmount):

```java
// JVM flag to detect pinning
// -Djdk.tracePinnedThreads=full

// Avoid synchronized blocks in virtual threads
// ✗ BAD - Pins the virtual thread
synchronized(lock) {
    // Blocking I/O here pins the virtual thread
    httpClient.send(request);
}

// ✓ GOOD - Use ReentrantLock
Lock lock = new ReentrantLock();
lock.lock();
try {
    // Blocking I/O - virtual thread can unmount
    httpClient.send(request);
} finally {
    lock.unlock();
}
```

## Migration Strategy

### Phase 1: Low-Risk Areas
1. Start with batch jobs and background tasks
2. Migrate async message processors
3. Update scheduled jobs

### Phase 2: Request Handling
1. Enable virtual threads in Spring Boot (Tomcat executor)
2. Monitor performance and resource usage
3. Adjust connection pool sizes

### Phase 3: Framework Integration
1. Update all ExecutorService usages
2. Review ThreadLocal usage
3. Replace synchronized with ReentrantLock where needed

## Performance Characteristics

| Aspect | Platform Threads | Virtual Threads |
|--------|-----------------|-----------------|
| Memory per thread | ~1MB | ~1KB |
| Max concurrent | ~Thousands | Millions |
| Creation cost | High | Very low |
| Context switch | OS-level | JVM-level |
| Blocking I/O cost | Thread wasted | Thread parked |

## Monitoring and Observability

```java
// Enable JFR events for virtual threads
// -XX:StartFlightRecording:settings=profile

// ThreadMXBean doesn't count virtual threads
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
int platformThreadCount = threadBean.getThreadCount();  // Only platform threads

// Use Thread.getAllStackTraces() for all threads including virtual
Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
```

## Common Pitfalls

1. **Expecting thread-per-core performance**: Virtual threads excel at I/O, not CPU-bound tasks
2. **Not adjusting connection pools**: Can increase pool sizes with virtual threads
3. **Using ThreadLocal extensively**: Expensive with millions of threads
4. **Synchronized blocks with I/O**: Causes thread pinning
5. **Old monitoring tools**: May not recognize virtual threads

## Framework Support

### Spring Boot 3.2+

```properties
# application.properties
spring.threads.virtual.enabled=true
```

### Quarkus

```properties
# application.properties
quarkus.virtual-threads.enabled=true
```

### Micronaut

```yaml
# application.yml
micronaut:
  executors:
    io:
      type: virtual
```

## Summary

Virtual threads are a paradigm shift for Java concurrency, enabling simple thread-per-request code to scale to millions of concurrent operations. They're especially powerful for I/O-heavy backend applications, eliminating the need for reactive programming in many cases.

**Migration Priority**: VERY HIGH - Game-changer for scalability. Requires JDK 21+, significant performance improvements for I/O-heavy workloads.
