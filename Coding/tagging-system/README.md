# Tagging System - Multi-Level Implementation

A comprehensive tagging system implementation showcasing different engineering levels and architectural approaches. This project demonstrates how the same core requirements can be implemented with varying levels of complexity, scalability, and enterprise-readiness.

## 📋 Core Requirements

- **Tag files** with multiple tags
- **Find top N tags** by frequency/usage
- **Query files** by tags
- **Clean code practices** with SOLID principles
- **Comprehensive testing** and validation

## 🏗️ Architecture Overview

### Three Implementation Levels:

1. **SE2 Basic** - Simple, functional implementation
2. **Staff Advanced** - Enterprise features with caching and events
3. **Principal Distributed** - Scalable, distributed architecture

## 📁 Project Structure

```
tagging-system/
├── se2-basic/              # Software Engineer 2 Level
├── staff-advanced/         # Staff Software Engineer Level
├── principal-distributed/  # Principal Engineer Level
└── README.md              # This file
```

## 🔧 Technology Stack

- **Java 21** with preview features
- **Maven** for build management
- **JUnit 5** for testing
- **Mockito** for mocking
- **Jackson** for JSON processing (Principal level)
- **TestContainers** for integration testing (Principal level)

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Maven 3.8+

### Build All Packages
```bash
# Build SE2 Basic
cd se2-basic && mvn clean compile test

# Build Staff Advanced
cd ../staff-advanced && mvn clean compile test

# Build Principal Distributed
cd ../principal-distributed && mvn clean compile test
```

## 📊 Implementation Comparison

| Feature | SE2 Basic | Staff Advanced | Principal Distributed |
|---------|-----------|----------------|----------------------|
| **Core Functionality** | ✅ | ✅ | ✅ |
| **Thread Safety** | ❌ | ✅ | ✅ |
| **Caching** | ❌ | ✅ | ✅ |
| **Events/Observers** | ❌ | ✅ | ✅ |
| **Complex Queries** | ❌ | ✅ | ✅ |
| **Validation** | Basic | Advanced | Enterprise |
| **Persistence** | ❌ | ❌ | ✅ |
| **Distribution** | ❌ | ❌ | ✅ |
| **Async Operations** | ❌ | ❌ | ✅ |
| **Metrics & Monitoring** | ❌ | ❌ | ✅ |
| **Fault Tolerance** | ❌ | ❌ | ✅ |

## 📚 Detailed Documentation

### SE2 Basic Level
- **Focus**: Core functionality with clean code
- **Data Structures**: HashMap, HashSet, ArrayList
- **Algorithms**: Simple counting and sorting
- **Patterns**: Basic OOP principles
- **Testing**: Unit tests with JUnit 5

**Key Features:**
- File tagging and tag removal
- Top N tags calculation
- Basic input validation
- Clean, readable code

### Staff Advanced Level
- **Focus**: Enterprise features and extensibility
- **Concurrency**: ReadWriteLock for thread safety
- **Caching**: Time-based cache with invalidation
- **Events**: Observer pattern for tag operations
- **Validation**: Advanced input validation with custom rules
- **Storage**: Pluggable storage abstraction

**Key Features:**
- Thread-safe operations
- Event-driven architecture
- Caching with TTL
- Complex query support
- Pluggable storage backends
- Advanced validation

### Principal Distributed Level
- **Focus**: Scalable, distributed architecture
- **Distribution**: Consistent hashing for partitioning
- **Replication**: Multi-node replication with consensus
- **Async**: CompletableFuture-based operations
- **Monitoring**: Metrics collection and health checks
- **Fault Tolerance**: Graceful failure handling

**Key Features:**
- Horizontal scalability
- Consistent hashing partitioning
- Asynchronous operations with virtual threads
- Replication and fault tolerance
- Cluster management
- Comprehensive metrics
- Persistence layer
- Health monitoring

## 🧪 Testing Strategy

### SE2 Basic
- Unit tests covering core functionality
- Input validation testing
- Edge case handling

### Staff Advanced
- Unit tests with mocking
- Concurrency testing
- Cache behavior verification
- Event system testing

### Principal Distributed
- Unit tests with comprehensive mocking
- Integration tests with TestContainers
- Performance testing
- Chaos engineering tests
- Distributed system testing

## 🏛️ Design Patterns Used

### SE2 Basic
- **Single Responsibility Principle**
- **Method chaining for builder pattern**
- **Record classes for immutable data**

### Staff Advanced
- **Strategy Pattern** (Storage backends)
- **Observer Pattern** (Event listeners)
- **Cache-Aside Pattern**
- **Template Method Pattern**
- **Dependency Injection**

### Principal Distributed
- **Builder Pattern** (System configuration)
- **Command Pattern** (Tag operations)
- **Circuit Breaker Pattern**
- **CQRS Pattern** (Command Query Responsibility Segregation)
- **Saga Pattern** (Distributed transactions)

## 📈 Performance Characteristics

| Operation | SE2 Basic | Staff Advanced | Principal Distributed |
|-----------|-----------|----------------|----------------------|
| **Tag File** | O(k) | O(k) + cache invalidation | O(k) + replication |
| **Get File Tags** | O(1) | O(1) + cache lookup | O(1) distributed |
| **Top N Tags** | O(t log t) | O(t log t) + caching | O(t log t) aggregated |
| **Search Files** | N/A | O(t) | O(t) distributed |

Where: k = number of tags, t = total unique tags

## 🔒 Security Considerations

### SE2 Basic
- Basic input validation
- No sensitive data handling

### Staff Advanced
- Advanced input sanitization
- Thread-safe operations
- Event listener isolation

### Principal Distributed
- Distributed authentication
- Secure inter-node communication
- Audit logging
- Rate limiting
- Access control

## 📋 Code Quality

All implementations follow:
- **SOLID Principles**
- **Clean Code practices**
- **Comprehensive test coverage**
- **JavaDoc documentation**
- **Static analysis (SpotBugs, Checkstyle)**
- **Dependency analysis**

## 🚦 Getting Started Examples

### SE2 Basic Usage
```java
TaggingSystem system = new TaggingSystem();
system.tagFile("/docs/java-guide.pdf", List.of("java", "programming", "tutorial"));
List<TagCount> topTags = system.getTopNTags(10);
```

### Staff Advanced Usage
```java
TaggingSystem system = new TaggingSystem();
system.addListener(event -> System.out.println("Tag event: " + event));
system.tagFile("/docs/java-guide.pdf", List.of("java", "programming"));

TagQuery query = TagQuery.withRequiredTags("java", "programming")
                        .excluding("deprecated");
List<String> files = system.searchFiles(query);
```

### Principal Distributed Usage
```java
DistributedTaggingSystem system = new DistributedTaggingSystem.Builder()
    .clusterManager(clusterManager)
    .partitionStrategy(new ConsistentHashingStrategy(150))
    .replicationManager(replicationManager)
    .persistenceManager(persistenceManager)
    .metricsCollector(metricsCollector)
    .replicationFactor(3)
    .build();

CompletableFuture<Void> future = system.tagFileAsync("/docs/java-guide.pdf",
                                                   List.of("java", "programming"));
```

## 🎯 Interview Talking Points

### For SE2 Level
- Clean, readable code structure
- Basic algorithmic thinking
- Understanding of core data structures
- Simple testing approaches

### For Staff Level
- System design thinking
- Understanding of concurrency
- Event-driven architecture
- Caching strategies
- Extensible design patterns

### For Principal Level
- Distributed systems concepts
- Scalability and performance
- Fault tolerance and reliability
- Monitoring and observability
- Complex system orchestration

## 📝 License

This project is created for educational and interview purposes.