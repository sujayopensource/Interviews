# Staff Advanced Tagging System

## 🎯 Solution Overview

This implementation showcases **Staff Software Engineer level** thinking with focus on enterprise-ready features, extensible architecture, and production concerns. The solution demonstrates advanced design patterns, concurrency handling, caching strategies, and event-driven architecture.

## 🏗️ Architecture & Design

### Core Design Philosophy
- **Enterprise Ready**: Thread-safe, performant, and observable
- **Extensible**: Pluggable components and clear interfaces
- **Production Focused**: Caching, events, and monitoring hooks
- **SOLID Principles**: Clean architecture with proper separation of concerns

### Layered Architecture

```
┌─────────────────────────────────────────────────────┐
│                 API Layer                           │
│  TaggingSystem (Facade + Coordination)             │
├─────────────────────────────────────────────────────┤
│                Business Logic                       │
│  ├── Validation    ├── Events       ├── Caching    │
│  └── Query Engine  └── Concurrency  └── Metrics    │
├─────────────────────────────────────────────────────┤
│                Storage Layer                        │
│  TagStorage Interface + Implementations             │
└─────────────────────────────────────────────────────┘
```

### Component Architecture

#### 1. Core System (`TaggingSystem`)
```java
public class TaggingSystem {
    private final TagStorage storage;              // Strategy Pattern
    private final TagCountCache cache;             // Cache Layer
    private final TagValidator validator;          // Validation Layer
    private final Set<TagEventListener> listeners; // Observer Pattern
    private final ReadWriteLock lock;             // Concurrency Control
}
```

#### 2. Storage Abstraction (`TagStorage`)
```java
public interface TagStorage {
    Set<String> addTagsToFile(String filePath, Set<String> tags);
    boolean removeTagFromFile(String filePath, String tag);
    List<String> searchFiles(TagQuery query);
    // ... other operations
}
```

#### 3. Event System (`TagEventListener`)
```java
public interface TagEventListener {
    void onTagEvent(TagEvent event);
}

public record TagEvent(Type type, String filePath, String tag, Instant timestamp) {
    public enum Type { TAG_ADDED, TAG_REMOVED }
}
```

## 🎨 Design Patterns Applied

### 1. Strategy Pattern - Storage Backends
```java
// Pluggable storage implementations
TaggingSystem system = new TaggingSystem(new InMemoryTagStorage());
// Could easily be: new DatabaseTagStorage(), new RedisTagStorage(), etc.
```

**Benefits**:
- **Runtime Flexibility**: Switch storage backends without code changes
- **Testing**: Easy to mock storage for unit tests
- **Extensibility**: Add new storage types without modifying core logic

### 2. Observer Pattern - Event System
```java
TagEventListener auditLogger = event ->
    log.info("Tag {} {} for file {}", event.tag(), event.type(), event.filePath());

system.addListener(auditLogger);
system.addListener(metricsCollector);
system.addListener(notificationSender);
```

**Benefits**:
- **Loose Coupling**: Core system doesn't know about specific listeners
- **Extensibility**: Add new behaviors without modifying core code
- **Monitoring**: Built-in hooks for observability

### 3. Template Method Pattern - Validation
```java
public class TagValidator {
    public Set<String> normalizeTags(List<String> tags) {
        return tags.stream()
                .map(this::normalizeTag)      // Template step
                .filter(this::isValidTag)     // Template step
                .collect(Collectors.toSet());
    }
}
```

### 4. Cache-Aside Pattern - Performance Optimization
```java
public List<TagCount> getTopNTags(int n) {
    return cache.getOrCompute(() -> {
        // Expensive computation only when cache miss
        return computeTopTags(n);
    });
}
```

## 🧵 Concurrency Design

### Thread Safety Strategy

#### 1. ReadWriteLock for Reader-Writer Pattern
```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();

public void tagFile(String filePath, List<String> tags) {
    lock.writeLock().lock();
    try {
        // Exclusive write access
        storage.addTagsToFile(filePath, normalizedTags);
        cache.invalidateCache();
    } finally {
        lock.writeLock().unlock();
    }
}

public List<String> getFileTags(String filePath) {
    lock.readLock().lock();
    try {
        // Concurrent read access
        return storage.getFileTags(filePath);
    } finally {
        lock.readLock().unlock();
    }
}
```

**Benefits**:
- **High Read Concurrency**: Multiple readers can access simultaneously
- **Write Safety**: Exclusive access during modifications
- **Performance**: Better than synchronized for read-heavy workloads

#### 2. Thread-Safe Collections
```java
private final Set<TagEventListener> listeners = ConcurrentHashMap.newKeySet();
```

**Benefits**:
- **Lock-Free**: ConcurrentHashMap uses advanced concurrency techniques
- **Performance**: Better throughput under contention
- **Safety**: Thread-safe without explicit synchronization

### Concurrency Patterns Used

1. **Reader-Writer Lock**: Optimized for read-heavy workloads
2. **Lock-Free Collections**: ConcurrentHashMap for listener management
3. **Defensive Copying**: Return immutable views to prevent external modification
4. **Cache Invalidation**: Thread-safe cache management

## 💾 Caching Architecture

### Cache Implementation
```java
public class TagCountCache {
    private volatile List<TagCount> cachedTopTags;
    private volatile long lastCacheTime;
    private final long cacheExpirationMs;
    private final ReentrantLock cacheLock;
}
```

#### Cache Strategy: Time-Based Expiration
```java
public List<TagCount> getOrCompute(Supplier<List<TagCount>> supplier) {
    // Double-checked locking pattern
    if (cachedTopTags != null && !isExpired()) {
        return cachedTopTags;  // Cache hit
    }

    cacheLock.lock();
    try {
        // Double-check after acquiring lock
        if (cachedTopTags != null && !isExpired()) {
            return cachedTopTags;
        }

        // Cache miss - compute and store
        cachedTopTags = supplier.get();
        lastCacheTime = System.currentTimeMillis();
        return cachedTopTags;
    } finally {
        cacheLock.unlock();
    }
}
```

**Benefits**:
- **Performance**: Avoid expensive recomputation
- **TTL-Based**: Automatic expiration prevents stale data
- **Thread-Safe**: Double-checked locking for performance
- **Invalidation**: Manual invalidation on data changes

### Cache Invalidation Strategy
- **Write-Through**: Invalidate on every write operation
- **Eager Invalidation**: Clear cache immediately on data changes
- **TTL Backup**: Time-based expiration as safety net

## 🔍 Advanced Query Engine

### Query DSL Implementation
```java
public class TagQuery {
    private final Set<String> requiredTags;  // AND operation
    private final Set<String> optionalTags;  // OR operation
    private final Set<String> excludedTags;  // NOT operation

    // Fluent API for query building
    public static TagQuery withRequiredTags(String... tags) { ... }
    public TagQuery excluding(String... tags) { ... }
}
```

### Query Examples
```java
// Find files with Java AND Programming, but exclude Tutorial
TagQuery query = TagQuery.withRequiredTags("java", "programming")
                         .excluding("tutorial");

// Find files with any of these tags
TagQuery query = TagQuery.withOptionalTags("java", "python", "scala");

// Complex query: (Java OR Python) AND Programming BUT NOT Beginner
TagQuery query = new TagQuery(
    Set.of("programming"),           // required
    Set.of("java", "python"),        // optional (OR)
    Set.of("beginner")               // excluded
);
```

### Query Processing Algorithm
```java
public List<String> searchFiles(TagQuery query) {
    Set<String> result = new HashSet<>();

    // Start with required tags (AND operation)
    for (String tag : query.getRequiredTags()) {
        Set<String> filesWithTag = tagToFiles.get(tag);
        if (result.isEmpty()) {
            result.addAll(filesWithTag);
        } else {
            result.retainAll(filesWithTag);  // Intersection
        }
    }

    // Add optional tags (OR operation)
    for (String tag : query.getOptionalTags()) {
        result.addAll(tagToFiles.get(tag));  // Union
    }

    // Remove excluded tags (NOT operation)
    for (String tag : query.getExcludedTags()) {
        result.removeAll(tagToFiles.get(tag));  // Difference
    }

    return new ArrayList<>(result);
}
```

## ✅ Validation & Input Sanitization

### Advanced Validation Rules
```java
public class TagValidator {
    private static final int MAX_TAG_LENGTH = 100;
    private static final String INVALID_CHARACTERS = "[^a-zA-Z0-9_-]";

    public String normalizeTag(String tag) {
        String normalized = tag.trim().toLowerCase();

        // Length validation
        if (normalized.length() > MAX_TAG_LENGTH) {
            throw new IllegalArgumentException("Tag too long");
        }

        // Character validation
        if (normalized.matches(".*" + INVALID_CHARACTERS + ".*")) {
            throw new IllegalArgumentException("Invalid characters in tag");
        }

        return normalized;
    }
}
```

### Validation Strategy
- **Fail Fast**: Immediate validation on input
- **Consistent Rules**: Same validation across all entry points
- **Security Focused**: Prevent injection attacks
- **User Friendly**: Clear error messages

## 📊 Performance Optimizations

### Time Complexity Analysis

| Operation | Without Cache | With Cache | Concurrent |
|-----------|---------------|------------|------------|
| `tagFile` | O(k) | O(k) | O(k) + lock |
| `getTopNTags` | O(t log t) | O(1) cache hit | O(1) read lock |
| `searchFiles` | O(r + o + e) | N/A | O(r + o + e) + lock |

Where: k=tags added, t=total tags, r=required tags, o=optional tags, e=excluded tags

### Space Complexity
- **Base Storage**: O(F × T) - same as basic implementation
- **Cache Overhead**: O(N) for top N tags cache
- **Event Overhead**: O(L) for L listeners
- **Lock Overhead**: Minimal per-instance overhead

### Performance Features
1. **Smart Caching**: Reduces expensive computations
2. **Read Optimization**: ReadWriteLock allows concurrent reads
3. **Lazy Evaluation**: Compute only when needed
4. **Efficient Queries**: Set operations for complex searches

## 🔄 Event-Driven Architecture

### Event Types & Data
```java
public record TagEvent(Type type, String filePath, String tag, Instant timestamp) {
    public enum Type {
        TAG_ADDED,    // When tag is added to file
        TAG_REMOVED   // When tag is removed from file
    }
}
```

### Event Processing
```java
private void notifyListeners(TagEvent event) {
    for (TagEventListener listener : listeners) {
        try {
            listener.onTagEvent(event);
        } catch (Exception e) {
            // Isolate listener failures - don't break system
            log.error("Listener failed", e);
        }
    }
}
```

### Event Use Cases
1. **Audit Logging**: Track all tag operations
2. **Metrics Collection**: Gather usage statistics
3. **Cache Warming**: Precompute popular queries
4. **Notifications**: Alert on important tag changes
5. **Search Indexing**: Update external search systems

## 🏭 Production Considerations

### Error Handling Strategy
```java
public void tagFile(String filePath, List<String> tags) {
    try {
        validator.validateFilePath(filePath);
        validator.validateTags(tags);

        Set<String> normalizedTags = validator.normalizeTags(tags);

        lock.writeLock().lock();
        try {
            Set<String> addedTags = storage.addTagsToFile(filePath, normalizedTags);
            cache.invalidateCache();

            // Fire events for successfully added tags
            for (String tag : addedTags) {
                notifyListeners(new TagEvent(TagEvent.Type.TAG_ADDED, filePath, tag));
            }
        } finally {
            lock.writeLock().unlock();
        }
    } catch (Exception e) {
        // Log error but let it propagate
        log.error("Failed to tag file: " + filePath, e);
        throw e;
    }
}
```

### Monitoring Hooks
- **Event System**: Built-in observability
- **Exception Handling**: Proper logging and propagation
- **Performance Metrics**: Cache hit rates, operation times
- **Health Checks**: System state verification

### Configuration Management
- **Cache TTL**: Configurable expiration times
- **Validation Rules**: Pluggable validation strategies
- **Storage Backend**: Runtime storage selection
- **Thread Pool**: Configurable concurrency levels

## 🧪 Testing Strategy

### Test Categories

#### 1. Unit Tests with Mocking
```java
@Mock private TagStorage mockStorage;
@Mock private TagEventListener mockListener;

@Test
void shouldInvalidateCacheOnTagAddition() {
    // Test cache invalidation behavior
}
```

#### 2. Concurrency Tests
```java
@Test
void shouldHandleConcurrentOperationsSafely() {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(10);

    // Launch 10 concurrent operations
    // Verify thread safety
}
```

#### 3. Integration Tests
```java
@Test
void shouldTriggerEventsAndUpdateCache() {
    // Test full workflow: validation → storage → cache → events
}
```

#### 4. Performance Tests
```java
@Test
void shouldCacheTopTagsEffectively() {
    // Measure cache hit/miss rates
    // Verify performance improvements
}
```

## 💡 Extension Points

### Easy Extensions
1. **Storage Backends**: Database, Redis, File system
2. **Event Handlers**: Elasticsearch indexing, metrics collection
3. **Validation Rules**: Custom business rules
4. **Cache Strategies**: LRU, size-based, distributed caching

### Advanced Extensions
1. **Distributed Events**: Message queues for cross-service communication
2. **Complex Queries**: Full-text search, fuzzy matching
3. **Analytics**: Tag usage patterns, recommendations
4. **Multi-tenancy**: Isolation between different users/organizations

## 🎯 Interview Discussion Points

### Architecture Decisions
- **Why ReadWriteLock?** Optimized for read-heavy workloads
- **Why Strategy Pattern for storage?** Runtime flexibility and testability
- **Why cache invalidation over TTL only?** Data consistency guarantees
- **Why event system?** Extensibility and observability

### Trade-offs Made
- **Memory vs Performance**: Cache uses memory for speed
- **Complexity vs Features**: More complex but enterprise-ready
- **Consistency vs Performance**: Strong consistency over eventual consistency

### Production Readiness
- **Concurrency**: Thread-safe with optimal read performance
- **Observability**: Events and monitoring hooks
- **Extensibility**: Pluggable components
- **Error Handling**: Robust exception management

### Scalability Considerations
- **Vertical Scaling**: Efficient use of multiple cores
- **Cache Strategy**: Reduces computational load
- **Event Decoupling**: Enables horizontal scaling of event processing
- **Storage Abstraction**: Enables distributed storage backends

This implementation demonstrates Staff-level engineering: advanced architecture patterns, production concerns, performance optimization, and extensible design.