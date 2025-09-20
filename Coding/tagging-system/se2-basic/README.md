# SE2 Basic Tagging System

## 🎯 Solution Overview

This implementation demonstrates **Software Engineer 2 level** thinking with focus on clean, functional code using fundamental data structures and algorithms. The solution prioritizes readability, correctness, and proper use of Java collections.

## 🏗️ Architecture & Design

### Core Design Philosophy
- **Simplicity First**: Use the most straightforward approach that works
- **Clean Code**: Readable, well-structured implementation
- **Functional Correctness**: All requirements met with proper edge case handling
- **Java Best Practices**: Proper use of collections and modern Java features

### Data Model

```java
public class TaggingSystem {
    // Bidirectional mapping for efficient queries
    private final Map<String, Set<String>> fileToTags;    // File -> Tags
    private final Map<String, Set<String>> tagToFiles;    // Tag -> Files
}
```

#### Why This Design?

1. **HashMap for O(1) Access**: Direct file/tag lookups
2. **HashSet for Uniqueness**: Automatic duplicate tag handling
3. **Bidirectional Mapping**: Efficient queries in both directions
4. **Memory Efficiency**: Shared string references, no data duplication

### Key Design Decisions

#### 1. Data Structure Choice
```java
// Primary storage: HashMap + HashSet
private final Map<String, Set<String>> fileToTags = new HashMap<>();
private final Map<String, Set<String>> tagToFiles = new HashMap<>();
```

**Rationale**:
- **HashMap**: O(1) average lookup/insert/delete
- **HashSet**: O(1) average operations with automatic uniqueness
- **Memory Efficient**: No redundant string storage

#### 2. Tag Normalization Strategy
```java
String normalizedTag = tag.trim().toLowerCase();
```

**Rationale**:
- **Consistency**: "Work", "work", " WORK " all become "work"
- **User-Friendly**: Case-insensitive matching
- **Clean Data**: Automatic whitespace trimming

#### 3. Input Validation Pattern
```java
private void validateFilePath(String filePath) {
    if (filePath == null || filePath.trim().isEmpty()) {
        throw new IllegalArgumentException("File path cannot be null or empty");
    }
}
```

**Rationale**:
- **Fail Fast**: Immediate feedback on invalid input
- **Clear Messages**: Descriptive error messages for debugging
- **Consistent API**: All methods validate inputs uniformly

## 🔍 Algorithm Analysis

### Time Complexity

| Operation | Time Complexity | Explanation |
|-----------|----------------|-------------|
| `tagFile(file, tags)` | **O(k)** | k = number of tags to add |
| `removeTagFromFile(file, tag)` | **O(1)** | Direct hash lookup and removal |
| `getFileTags(file)` | **O(1)** | Direct hash lookup |
| `getFilesWithTag(tag)` | **O(1)** | Direct hash lookup |
| `getTopNTags(n)` | **O(t log t)** | t = total unique tags, due to sorting |
| `getTagCount(tag)` | **O(1)** | Direct hash lookup |

### Space Complexity

- **Overall**: **O(F × T)** where F = files, T = average tags per file
- **Storage**: Each file-tag relationship stored twice (bidirectional)
- **Overhead**: HashMap overhead + HashSet overhead per collection

### Algorithm Deep Dive

#### Top N Tags Implementation
```java
public List<TagCount> getTopNTags(int n) {
    return tagToFiles.entrySet().stream()
            .map(entry -> new TagCount(entry.getKey(), entry.getValue().size()))
            .sorted((a, b) -> Integer.compare(b.count(), a.count()))  // Descending
            .limit(n)
            .collect(Collectors.toList());
}
```

**Algorithm Choice**:
- **Stream-based**: Leverages Java 8+ functional programming
- **Sort-then-limit**: Simple approach for small-medium datasets
- **Space Efficient**: No intermediate data structures

**Alternative Considered**: Priority Queue (Min-Heap)
- **When Better**: For very large tag sets with small N
- **Complexity**: O(t log n) vs O(t log t)
- **Decision**: Stream approach is clearer for SE2 level

## 🧩 Design Patterns & Principles

### SOLID Principles Applied

#### 1. Single Responsibility Principle (SRP)
```java
// Each method has one clear responsibility
public void tagFile(String filePath, List<String> tags) { ... }        // Only tags files
public List<String> getFileTags(String filePath) { ... }               // Only retrieves tags
private void validateFilePath(String filePath) { ... }                 // Only validates
```

#### 2. Open/Closed Principle (OCP)
- **Extensible**: Can add new operations without modifying existing code
- **Stable**: Core data structures remain unchanged

#### 3. Interface Segregation Principle (ISP)
- **Focused API**: Only methods clients need
- **No Fat Interfaces**: Each method serves a specific purpose

### Modern Java Features Used

#### 1. Records for Immutable Data
```java
public record TagCount(String tag, int count) {}
```

**Benefits**:
- **Immutable**: Thread-safe data containers
- **Concise**: Automatic equals/hashCode/toString
- **Type-Safe**: Compile-time guarantees

#### 2. Stream API for Data Processing
```java
Set<String> normalizedTags = tags.stream()
        .map(String::trim)
        .map(String::toLowerCase)
        .filter(tag -> !tag.isEmpty())
        .collect(Collectors.toSet());
```

**Benefits**:
- **Functional**: Declarative data processing
- **Readable**: Clear intent and flow
- **Composable**: Easy to modify and extend

#### 3. Collections Factory Methods
```java
return new ArrayList<>(fileToTags.getOrDefault(filePath, Collections.emptySet()));
```

**Benefits**:
- **Safe Defaults**: Avoid null pointer exceptions
- **Defensive Copying**: Prevent external modification

## 🔄 Data Flow & State Management

### Tag Addition Flow
```
1. Input Validation
   ├── File path validation
   └── Tags validation

2. Tag Normalization
   ├── Trim whitespace
   ├── Convert to lowercase
   └── Filter empty tags

3. Data Update (Atomic)
   ├── Update fileToTags mapping
   └── Update tagToFiles mapping
```

### State Consistency Guarantees
- **Bidirectional Sync**: File-tag relationships always consistent
- **Atomic Updates**: Either both mappings update or neither
- **Clean State**: Empty collections automatically removed

## ⚡ Performance Characteristics

### Strengths
- **Fast Lookups**: O(1) for most operations
- **Memory Efficient**: Minimal object overhead
- **Simple**: Easy to understand and debug

### Limitations & Trade-offs
- **Top N Tags**: O(t log t) - could be optimized with heaps
- **No Caching**: Recalculates everything on each call
- **Memory Growth**: No cleanup of empty collections (fixed in implementation)
- **Single-Threaded**: No concurrency protection

### Scalability Analysis

#### Suitable For:
- **Small to Medium**: Up to 100K files, 10K unique tags
- **Development**: Prototyping and initial implementations
- **Simple Use Cases**: Basic tagging without complex queries

#### Limitations:
- **Memory Bound**: All data in memory
- **CPU Intensive**: Top N requires sorting on each call
- **No Persistence**: Data lost on restart

## 🧪 Testing Strategy

### Test Categories

#### 1. Functional Tests
```java
@Test
void shouldTagFileSuccessfully() {
    // Test basic functionality works
}

@Test
void shouldNormalizeTagsToLowercase() {
    // Test tag normalization
}
```

#### 2. Edge Case Tests
```java
@Test
void shouldThrowExceptionForNullFilePath() {
    // Test input validation
}

@Test
void shouldHandleEmptyWhitespaceTags() {
    // Test edge cases
}
```

#### 3. Integration Tests
```java
@Test
void shouldMaintainConsistencyAfterOperations() {
    // Test bidirectional mapping consistency
}
```

### Test Design Philosophy
- **Clear Names**: Test method names describe exact scenario
- **Single Responsibility**: One assertion per test method
- **Edge Cases**: Cover boundary conditions
- **Happy Path**: Verify normal operations work

## 💡 Extension Points

### Easy Extensions
1. **File Metadata**: Add file size, creation date, etc.
2. **Tag Metadata**: Add tag descriptions, colors, etc.
3. **Batch Operations**: Tag multiple files at once
4. **Export/Import**: JSON serialization support

### Architectural Improvements
1. **Caching**: Add top N tags cache
2. **Persistence**: Add file-based storage
3. **Concurrency**: Add thread safety
4. **Validation**: More sophisticated input validation

## 🎯 Interview Discussion Points

### Technical Decisions
- **Why HashMap over TreeMap?** O(1) vs O(log n) for most operations
- **Why bidirectional mapping?** Efficient queries in both directions
- **Why normalize tags?** User experience and data consistency

### Trade-offs Made
- **Memory vs CPU**: Store both mappings for faster queries
- **Simplicity vs Performance**: Clear code over micro-optimizations
- **Features vs Complexity**: Core functionality only

### Potential Improvements
- **Caching Strategy**: For frequently requested top N tags
- **Data Structures**: Priority queue for top N optimization
- **Memory Management**: Cleanup empty collections
- **Error Handling**: More specific exception types

### Scalability Considerations
- **Memory Limitations**: All data must fit in memory
- **Performance Bottlenecks**: Top N tags sorting
- **Concurrency**: Single-threaded design
- **Persistence**: No data durability

This implementation demonstrates solid SE2-level engineering: clean code, proper use of data structures, comprehensive testing, and clear documentation of design decisions.