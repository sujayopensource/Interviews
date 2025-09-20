# File Tagging System - Problem Statement

## 🎯 Problem Overview

Design and implement a **File Tagging System** that allows users to organize files using tags and efficiently retrieve information about tag usage. This is a common system design problem that tests understanding of data structures, algorithms, and scalable architecture.

## 📋 Core Requirements

### Functional Requirements

1. **Tag Files**: Associate multiple tags with file paths
   ```
   tagFile("/documents/report.pdf", ["work", "important", "quarterly"])
   ```

2. **Remove Tags**: Remove specific tags from files
   ```
   removeTagFromFile("/documents/report.pdf", "quarterly")
   ```

3. **Query File Tags**: Retrieve all tags associated with a file
   ```
   getFileTags("/documents/report.pdf") → ["work", "important"]
   ```

4. **Query Files by Tag**: Find all files that have a specific tag
   ```
   getFilesWithTag("work") → ["/documents/report.pdf", "/notes/meeting.txt"]
   ```

5. **Top N Tags**: Get the most frequently used tags
   ```
   getTopNTags(5) → [("work", 150), ("important", 89), ("personal", 67), ...]
   ```

6. **Tag Statistics**: Get usage count for specific tags
   ```
   getTagCount("work") → 150
   ```

### Non-Functional Requirements

1. **Performance**:
   - Tag operations should be efficient (preferably O(1) or O(log n))
   - Top N tags should be computed efficiently
   - Support for large numbers of files and tags

2. **Scalability**:
   - Handle millions of files and thousands of unique tags
   - Support concurrent operations
   - Horizontal scaling capability (for advanced implementations)

3. **Reliability**:
   - Data consistency
   - Fault tolerance (for distributed implementations)
   - Graceful error handling

4. **Maintainability**:
   - Clean, readable code
   - Proper separation of concerns
   - Extensible design

## 🔄 System Behavior

### Data Consistency Rules

1. **Bidirectional Mapping**: The system maintains consistency between:
   - File → Tags mapping
   - Tag → Files mapping

2. **Tag Normalization**:
   - Tags are case-insensitive ("Work" and "work" are the same)
   - Whitespace is trimmed
   - Empty tags are rejected

3. **Cleanup**: When a tag is removed from all files, it should be cleaned up from the system

4. **Duplicate Handling**: Adding the same tag to a file multiple times should be idempotent

### Edge Cases to Handle

1. **Empty Inputs**:
   - Null or empty file paths
   - Null or empty tag lists
   - Whitespace-only tags

2. **Invalid Operations**:
   - Removing non-existent tags
   - Querying non-existent files
   - Invalid parameters (negative N for top tags)

3. **Large Scale**:
   - Files with hundreds of tags
   - Tags applied to thousands of files
   - Memory efficiency with large datasets

## 🎓 Engineering Level Expectations

### Software Engineer 2 (SE2) Level
**Focus**: Demonstrate clean coding and basic algorithmic thinking

- **Data Structures**: Use appropriate collections (HashMap, HashSet, etc.)
- **Algorithms**: Efficient sorting and counting
- **Code Quality**: Clean, readable implementation
- **Testing**: Basic unit tests covering main functionality
- **Time Complexity**: Understand and implement efficient operations

**Key Concepts to Demonstrate**:
- SOLID principles
- Proper error handling
- Input validation
- Basic OOP design

### Staff Software Engineer Level
**Focus**: Show system design skills and enterprise-ready code

- **Concurrency**: Thread-safe operations
- **Caching**: Performance optimization strategies
- **Extensibility**: Pluggable components and interfaces
- **Observability**: Event systems and monitoring hooks
- **Advanced Queries**: Complex search capabilities
- **Validation**: Robust input validation and sanitization

**Key Concepts to Demonstrate**:
- Design patterns (Strategy, Observer, etc.)
- Concurrent programming
- Cache management
- Event-driven architecture
- Interface segregation

### Principal Engineer Level
**Focus**: Demonstrate distributed systems expertise and architectural leadership

- **Distribution**: Horizontal scaling across multiple nodes
- **Consistency**: Distributed consensus and replication
- **Fault Tolerance**: Graceful failure handling
- **Performance**: Async operations and optimization
- **Monitoring**: Comprehensive metrics and health checks
- **Persistence**: Durable storage and recovery

**Key Concepts to Demonstrate**:
- Distributed systems patterns
- Consensus algorithms
- Partitioning strategies (consistent hashing)
- Async programming models
- System monitoring and observability

## 📊 Sample Data Scenarios

### Small Scale (SE2 Level)
```
Files: 100-1000
Tags per file: 1-10
Unique tags: 50-200
Operations/second: 10-100
```

### Medium Scale (Staff Level)
```
Files: 10K-100K
Tags per file: 5-20
Unique tags: 500-2000
Operations/second: 100-1000
Concurrent users: 10-50
```

### Large Scale (Principal Level)
```
Files: 1M-10M+
Tags per file: 10-50
Unique tags: 5K-50K
Operations/second: 1000-10000+
Concurrent users: 100-1000+
Geographic distribution: Multi-region
```

## 🧪 Testing Scenarios

### Basic Functionality Tests
- Tag assignment and retrieval
- Tag removal and cleanup
- Top N tags calculation
- File querying by tags

### Edge Case Tests
- Empty and null inputs
- Special characters in file paths
- Very long tag names
- Duplicate operations

### Performance Tests
- Large dataset operations
- Concurrent access patterns
- Memory usage optimization
- Response time under load

### System Tests (Advanced Levels)
- Failure scenarios
- Network partitions
- Data corruption recovery
- Scaling operations

## 🎯 Success Criteria

### Code Quality
- ✅ Clean, readable code structure
- ✅ Proper error handling and validation
- ✅ Comprehensive test coverage
- ✅ Documentation and comments where needed

### Functionality
- ✅ All core operations work correctly
- ✅ Efficient algorithms and data structures
- ✅ Proper handling of edge cases
- ✅ Consistent behavior under various conditions

### Architecture (Advanced Levels)
- ✅ Scalable design patterns
- ✅ Proper separation of concerns
- ✅ Extensible and maintainable structure
- ✅ Performance optimization strategies

### Interview Discussion Points
- Design decisions and trade-offs
- Alternative approaches considered
- Scalability bottlenecks and solutions
- Real-world deployment considerations
- Monitoring and operational concerns

## 💡 Extension Ideas

For advanced discussions, consider these extensions:

1. **Hierarchical Tags**: Support for tag hierarchies (`work/projects/urgent`)
2. **Tag Relationships**: Semantic relationships between tags
3. **Versioning**: Track tag changes over time
4. **Access Control**: User-based tag permissions
5. **Analytics**: Tag usage patterns and recommendations
6. **Integration**: APIs for external systems
7. **Backup/Recovery**: Data persistence and disaster recovery
8. **Multi-tenancy**: Support for multiple organizations

This problem statement provides a comprehensive foundation for implementing and discussing file tagging systems at various levels of complexity and scale.