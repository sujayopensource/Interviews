# Version Control System for Documentation - Staff Software Engineer Level

## Problem Statement & Requirements Analysis

### Business Context
Design a scalable, enterprise-grade version control system for documentation that supports large organizations with thousands of technical writers, product managers, and engineers collaborating on complex documentation workflows.

### Functional Requirements

#### Core Version Control
1. **Advanced Document Management**
   - Support for rich media (images, videos, diagrams)
   - Multi-format support (Markdown, AsciiDoc, reStructuredText, DITA)
   - Binary file handling with LFS (Large File Support)
   - Document templates and scaffolding

2. **Sophisticated Version Control**
   - Git-like branching model with documentation-specific optimizations
   - Advanced merge strategies for documentation workflows
   - Semantic versioning for documentation releases
   - Tag-based releases with automated changelog generation

3. **Enterprise Collaboration**
   - Review workflows with approval processes
   - Role-based access control with fine-grained permissions
   - Integration with LDAP/SSO systems
   - Audit trails and compliance features

4. **Publishing & Integration**
   - Multi-target publishing (web, PDF, mobile, API docs)
   - CI/CD integration for automated documentation builds
   - Custom themes and branding
   - Analytics and usage tracking

### Non-Functional Requirements
1. **Scale**: 100K+ documents, 10K+ concurrent users, 1K+ repositories
2. **Performance**: <100ms response time, real-time collaboration
3. **Availability**: 99.99% uptime with global distribution
4. **Security**: Enterprise-grade security, SOC 2 compliance

## System Architecture

### High-Level Architecture
```
                    ┌─── CDN (Global) ───┐
                    │                    │
    [Users] ────────┼── Load Balancer ───┼── Web Tier
                    │                    │
                    └────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  Application    │
                    │     Tier        │
                    └─────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   ┌────▼────┐      ┌───────▼───────┐      ┌─────▼─────┐
   │Database │      │ File Storage  │      │  Search   │
   │ Cluster │      │   (Dist.)     │      │  Engine   │
   └─────────┘      └───────────────┘      └───────────┘
```

### Microservices Architecture

#### Core Services
1. **Document Service**
   - Document CRUD operations
   - Content processing and transformation
   - Rich media handling
   - Document validation and linting

2. **Version Control Service**
   - Git-like operations (commit, branch, merge)
   - Conflict resolution algorithms
   - Tree management and storage
   - Delta compression

3. **Collaboration Service**
   - Real-time editing capabilities
   - Comment and review systems
   - Notification management
   - Activity feeds

4. **Publishing Service**
   - Multi-format rendering
   - Static site generation
   - Theme management
   - Build pipeline orchestration

#### Supporting Services
5. **User Management Service**
   - Authentication and authorization
   - Role and permission management
   - Integration with external identity providers

6. **Search Service**
   - Full-text search with semantic capabilities
   - Faceted search and filtering
   - Search analytics and optimization

7. **Analytics Service**
   - Usage tracking and metrics
   - Performance monitoring
   - Business intelligence

8. **Integration Service**
   - External tool integrations
   - Webhook management
   - API gateway functionality

## Advanced Data Architecture

### Multi-Model Database Strategy

#### Primary Data Store (PostgreSQL)
```sql
-- Advanced repository structure
CREATE TABLE repositories (
    id UUID PRIMARY KEY,
    namespace VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    settings JSONB DEFAULT '{}',
    owner_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    archived_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT unique_repo_name UNIQUE(namespace, name)
);

-- Git-like object storage
CREATE TABLE git_objects (
    id UUID PRIMARY KEY,
    repository_id UUID REFERENCES repositories(id),
    object_type VARCHAR(20) NOT NULL, -- 'blob', 'tree', 'commit'
    content_hash VARCHAR(64) NOT NULL,
    content_size BIGINT NOT NULL,
    content_encoding VARCHAR(20),
    storage_path VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_object_hash UNIQUE(repository_id, content_hash)
);

-- Advanced commit structure
CREATE TABLE commits (
    id UUID PRIMARY KEY,
    repository_id UUID REFERENCES repositories(id),
    hash VARCHAR(64) UNIQUE NOT NULL,
    parent_hashes VARCHAR(64)[],
    tree_hash VARCHAR(64) NOT NULL,
    author_id UUID REFERENCES users(id),
    committer_id UUID REFERENCES users(id),
    message TEXT NOT NULL,
    metadata JSONB DEFAULT '{}',
    signed_data TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Branch and tag management
CREATE TABLE refs (
    id UUID PRIMARY KEY,
    repository_id UUID REFERENCES repositories(id),
    ref_type VARCHAR(20) NOT NULL, -- 'branch', 'tag'
    name VARCHAR(255) NOT NULL,
    target_hash VARCHAR(64) NOT NULL,
    protected BOOLEAN DEFAULT FALSE,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_ref_name UNIQUE(repository_id, ref_type, name)
);
```

#### Document-Specific Storage
```sql
-- Enhanced document metadata
CREATE TABLE documents (
    id UUID PRIMARY KEY,
    repository_id UUID REFERENCES repositories(id),
    path VARCHAR(1000) NOT NULL,
    title VARCHAR(500),
    description TEXT,
    content_type VARCHAR(100),
    language VARCHAR(10),
    tags VARCHAR(100)[],
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT unique_doc_path UNIQUE(repository_id, path)
);

-- Document relationships and dependencies
CREATE TABLE document_relations (
    id UUID PRIMARY KEY,
    source_document_id UUID REFERENCES documents(id),
    target_document_id UUID REFERENCES documents(id),
    relation_type VARCHAR(50) NOT NULL, -- 'includes', 'references', 'depends_on'
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Distributed File Storage

#### Content-Addressable Storage
```typescript
class ContentStore {
    private storage: DistributedFileSystem;
    private compression: CompressionService;

    async storeContent(content: Buffer, metadata: ContentMetadata): Promise<string> {
        // Generate content hash
        const hash = this.generateHash(content);

        // Compress content if beneficial
        const compressedContent = await this.compression.compress(content);

        // Generate storage path using hash sharding
        const storagePath = this.generateStoragePath(hash);

        // Store with replication
        await this.storage.store(storagePath, compressedContent, {
            replicationFactor: 3,
            metadata: metadata
        });

        return hash;
    }

    private generateStoragePath(hash: string): string {
        // Shard using first 4 characters of hash
        const shard = hash.substring(0, 4);
        return `objects/${shard.substring(0, 2)}/${shard.substring(2, 4)}/${hash}`;
    }
}
```

### Advanced Search Architecture

#### Elasticsearch Cluster Configuration
```typescript
interface DocumentSearchIndex {
    repository_id: string;
    document_id: string;
    path: string;
    title: string;
    content: string;
    content_type: string;
    language: string;
    tags: string[];
    author: string;
    last_modified: Date;
    semantic_embedding: number[]; // For semantic search
    relationships: {
        includes: string[];
        references: string[];
    };
}
```

#### Semantic Search Implementation
```typescript
class SemanticSearchService {
    private embeddingModel: EmbeddingModel;
    private vectorStore: VectorDatabase;

    async indexDocument(document: Document): Promise<void> {
        // Generate semantic embeddings
        const embedding = await this.embeddingModel.encode(document.content);

        // Store in vector database
        await this.vectorStore.store({
            id: document.id,
            embedding: embedding,
            metadata: {
                repository_id: document.repository_id,
                path: document.path,
                content_type: document.content_type
            }
        });
    }

    async semanticSearch(query: string, repositoryId: string): Promise<SearchResult[]> {
        const queryEmbedding = await this.embeddingModel.encode(query);

        const results = await this.vectorStore.search({
            embedding: queryEmbedding,
            filter: { repository_id: repositoryId },
            limit: 50,
            threshold: 0.7
        });

        return results.map(result => ({
            document_id: result.id,
            similarity_score: result.score,
            metadata: result.metadata
        }));
    }
}
```

## Advanced Version Control Implementation

### Git-Compatible Storage Model
```typescript
class GitCompatibleVCS {
    private objectStore: ContentStore;
    private refManager: ReferenceManager;

    async createCommit(
        repositoryId: string,
        changes: DocumentChange[],
        message: string,
        author: User
    ): Promise<Commit> {
        // Build tree objects for changed documents
        const treeObjects = await this.buildTreeObjects(repositoryId, changes);

        // Create commit object
        const commitObject = {
            tree: treeObjects.rootHash,
            parents: await this.getParentCommits(repositoryId),
            author: author,
            message: message,
            timestamp: new Date()
        };

        // Store commit object
        const commitHash = await this.objectStore.storeCommit(commitObject);

        // Update branch reference
        await this.refManager.updateBranch(repositoryId, 'main', commitHash);

        return {
            id: generateUUID(),
            hash: commitHash,
            ...commitObject
        };
    }

    async merge(
        repositoryId: string,
        sourceBranch: string,
        targetBranch: string,
        strategy: MergeStrategy = 'auto'
    ): Promise<MergeResult> {
        const sourceCommit = await this.getCommit(repositoryId, sourceBranch);
        const targetCommit = await this.getCommit(repositoryId, targetBranch);
        const baseCommit = await this.findMergeBase(sourceCommit, targetCommit);

        const conflicts = await this.detectConflicts(baseCommit, sourceCommit, targetCommit);

        if (conflicts.length === 0 || strategy === 'auto') {
            return await this.performAutoMerge(sourceCommit, targetCommit);
        }

        return {
            status: 'conflicts',
            conflicts: conflicts,
            mergeCommit: null
        };
    }
}
```

### Advanced Diff Algorithms
```typescript
class AdvancedDiffEngine {
    async computeStructuralDiff(
        oldDocument: ParsedDocument,
        newDocument: ParsedDocument
    ): Promise<StructuralDiff> {
        // Parse document structure (headings, sections, etc.)
        const oldStructure = await this.parseStructure(oldDocument);
        const newStructure = await this.parseStructure(newDocument);

        // Compute structural changes
        const structuralChanges = this.computeStructuralChanges(
            oldStructure,
            newStructure
        );

        // Compute content changes within unchanged structure
        const contentChanges = await this.computeContentChanges(
            oldDocument,
            newDocument,
            structuralChanges
        );

        return {
            structural: structuralChanges,
            content: contentChanges,
            similarity: this.computeSimilarity(oldDocument, newDocument)
        };
    }

    private async computeContentChanges(
        oldDoc: ParsedDocument,
        newDoc: ParsedDocument,
        structuralChanges: StructuralChange[]
    ): Promise<ContentChange[]> {
        // Use Myers' algorithm with semantic awareness
        const changes: ContentChange[] = [];

        for (const section of oldDoc.sections) {
            if (!this.wasStructurallyChanged(section, structuralChanges)) {
                const correspondingSection = this.findCorrespondingSection(
                    section,
                    newDoc.sections
                );

                if (correspondingSection) {
                    const diff = await this.computeSemanticDiff(
                        section.content,
                        correspondingSection.content
                    );
                    changes.push(...diff);
                }
            }
        }

        return changes;
    }
}
```

## Real-Time Collaboration System

### Operational Transformation
```typescript
class DocumentOTEngine {
    private operationHistory: Map<string, Operation[]> = new Map();

    async applyOperation(
        documentId: string,
        operation: Operation,
        clientId: string
    ): Promise<TransformResult> {
        const currentHistory = this.operationHistory.get(documentId) || [];

        // Transform operation against concurrent operations
        const transformedOp = await this.transformOperation(
            operation,
            currentHistory.slice(operation.sequenceNumber)
        );

        // Apply transformed operation
        const newContent = await this.applyTransformedOperation(
            documentId,
            transformedOp
        );

        // Add to history
        currentHistory.push(transformedOp);
        this.operationHistory.set(documentId, currentHistory);

        // Broadcast to other clients
        await this.broadcastOperation(documentId, transformedOp, clientId);

        return {
            success: true,
            newContent: newContent,
            transformedOperation: transformedOp
        };
    }

    private async transformOperation(
        operation: Operation,
        concurrentOps: Operation[]
    ): Promise<Operation> {
        let transformedOp = operation;

        for (const concurrentOp of concurrentOps) {
            transformedOp = await this.transformAgainst(transformedOp, concurrentOp);
        }

        return transformedOp;
    }
}
```

### WebSocket-Based Real-Time System
```typescript
class RealtimeCollaborationService {
    private documentSessions: Map<string, Set<WebSocket>> = new Map();
    private userCursors: Map<string, Map<string, CursorPosition>> = new Map();

    async handleConnection(ws: WebSocket, user: User, documentId: string): Promise<void> {
        // Add to document session
        if (!this.documentSessions.has(documentId)) {
            this.documentSessions.set(documentId, new Set());
        }
        this.documentSessions.get(documentId)!.add(ws);

        // Initialize user cursor tracking
        if (!this.userCursors.has(documentId)) {
            this.userCursors.set(documentId, new Map());
        }

        // Send current document state and active users
        await this.sendInitialState(ws, documentId);

        // Handle incoming messages
        ws.on('message', async (data) => {
            const message = JSON.parse(data.toString());
            await this.handleMessage(ws, user, documentId, message);
        });

        // Handle disconnection
        ws.on('close', () => {
            this.handleDisconnection(ws, user.id, documentId);
        });
    }

    private async handleMessage(
        ws: WebSocket,
        user: User,
        documentId: string,
        message: any
    ): Promise<void> {
        switch (message.type) {
            case 'operation':
                await this.handleOperation(documentId, message.operation, user.id);
                break;
            case 'cursor_update':
                await this.handleCursorUpdate(documentId, user.id, message.position);
                break;
            case 'selection_update':
                await this.handleSelectionUpdate(documentId, user.id, message.selection);
                break;
        }
    }
}
```

## Enterprise Features

### Advanced Access Control
```typescript
class AccessControlService {
    async evaluatePermission(
        user: User,
        resource: Resource,
        action: Action,
        context: SecurityContext
    ): Promise<boolean> {
        // Role-based access control
        const rolePermissions = await this.getRolePermissions(user.roles);

        // Resource-specific permissions
        const resourcePermissions = await this.getResourcePermissions(
            user.id,
            resource.id
        );

        // Attribute-based access control
        const attributeRules = await this.evaluateAttributeRules(
            user,
            resource,
            action,
            context
        );

        // Combine all permission sources
        return this.combinePermissions([
            rolePermissions,
            resourcePermissions,
            attributeRules
        ], action);
    }

    private async evaluateAttributeRules(
        user: User,
        resource: Resource,
        action: Action,
        context: SecurityContext
    ): Promise<Permission[]> {
        const rules = await this.getApplicableRules(resource.type, action);

        const results = await Promise.all(
            rules.map(rule => this.evaluateRule(rule, {
                user,
                resource,
                action,
                context,
                time: new Date(),
                ipAddress: context.ipAddress,
                userAgent: context.userAgent
            }))
        );

        return results.filter(result => result.granted);
    }
}
```

### Audit Trail System
```typescript
class AuditService {
    async logActivity(
        userId: string,
        action: string,
        resourceId: string,
        resourceType: string,
        details: any,
        context: AuditContext
    ): Promise<void> {
        const auditEvent = {
            id: generateUUID(),
            timestamp: new Date(),
            user_id: userId,
            action: action,
            resource_id: resourceId,
            resource_type: resourceType,
            details: details,
            ip_address: context.ipAddress,
            user_agent: context.userAgent,
            session_id: context.sessionId,
            success: true
        };

        // Store in primary audit log
        await this.storeAuditEvent(auditEvent);

        // Send to compliance systems
        await this.forwardToComplianceSystems(auditEvent);

        // Real-time monitoring
        await this.checkComplianceRules(auditEvent);
    }

    async generateComplianceReport(
        startDate: Date,
        endDate: Date,
        filters: ComplianceFilters
    ): Promise<ComplianceReport> {
        const events = await this.queryAuditEvents({
            start_date: startDate,
            end_date: endDate,
            ...filters
        });

        return {
            period: { start: startDate, end: endDate },
            total_events: events.length,
            user_activity: this.aggregateUserActivity(events),
            resource_access: this.aggregateResourceAccess(events),
            security_violations: this.detectSecurityViolations(events),
            compliance_status: this.assessCompliance(events)
        };
    }
}
```

## Publishing & Integration System

### Multi-Target Publishing
```typescript
class PublishingService {
    private generators: Map<string, ContentGenerator> = new Map();

    async publishDocumentation(
        repositoryId: string,
        target: PublishTarget,
        configuration: PublishConfig
    ): Promise<PublishResult> {
        const generator = this.generators.get(target.type);
        if (!generator) {
            throw new Error(`Unsupported publish target: ${target.type}`);
        }

        // Get source documents
        const documents = await this.getPublishableDocuments(
            repositoryId,
            configuration.branch || 'main'
        );

        // Process documents through pipeline
        const processedDocs = await this.processDocuments(documents, configuration);

        // Generate target format
        const output = await generator.generate(processedDocs, target.configuration);

        // Deploy to target
        const deployResult = await this.deployToTarget(output, target);

        return {
            success: true,
            target: target,
            deployedAt: new Date(),
            url: deployResult.url,
            artifacts: deployResult.artifacts
        };
    }

    private async processDocuments(
        documents: Document[],
        config: PublishConfig
    ): Promise<ProcessedDocument[]> {
        const pipeline = [
            new IncludeProcessor(),
            new VariableSubstitutionProcessor(),
            new LinkResolutionProcessor(),
            new ImageOptimizationProcessor(),
            new TableOfContentsProcessor(),
            new CrossReferenceProcessor()
        ];

        let processedDocs = documents;

        for (const processor of pipeline) {
            processedDocs = await processor.process(processedDocs, config);
        }

        return processedDocs;
    }
}
```

### CI/CD Integration
```typescript
class ContinuousIntegrationService {
    async setupBuildPipeline(
        repositoryId: string,
        configuration: CIPipelineConfig
    ): Promise<void> {
        const pipeline = {
            triggers: configuration.triggers,
            stages: [
                {
                    name: 'validation',
                    steps: [
                        { action: 'lint_documents', config: configuration.linting },
                        { action: 'check_links', config: configuration.linkChecking },
                        { action: 'spell_check', config: configuration.spellCheck }
                    ]
                },
                {
                    name: 'build',
                    steps: [
                        { action: 'generate_static_site', config: configuration.siteGeneration },
                        { action: 'optimize_assets', config: configuration.optimization }
                    ]
                },
                {
                    name: 'deploy',
                    steps: [
                        { action: 'deploy_to_staging', config: configuration.staging },
                        { action: 'run_tests', config: configuration.testing },
                        { action: 'deploy_to_production', config: configuration.production }
                    ]
                }
            ]
        };

        await this.storePipelineConfiguration(repositoryId, pipeline);
        await this.registerWebhooks(repositoryId, configuration.triggers);
    }

    async executePipeline(repositoryId: string, trigger: PipelineTrigger): Promise<PipelineExecution> {
        const pipeline = await this.getPipelineConfiguration(repositoryId);
        const execution = await this.createExecution(pipeline, trigger);

        try {
            for (const stage of pipeline.stages) {
                await this.executeStage(execution.id, stage);
            }

            execution.status = 'success';
        } catch (error) {
            execution.status = 'failed';
            execution.error = error.message;
        }

        await this.updateExecution(execution);
        await this.notifyStakeholders(execution);

        return execution;
    }
}
```

## Performance & Scalability

### Caching Strategy
```typescript
class CacheManager {
    private documentCache: DistributedCache;
    private searchCache: DistributedCache;
    private renderCache: DistributedCache;

    async getCachedDocument(
        repositoryId: string,
        documentPath: string,
        version?: string
    ): Promise<Document | null> {
        const cacheKey = this.buildDocumentCacheKey(repositoryId, documentPath, version);

        const cached = await this.documentCache.get(cacheKey);
        if (cached) {
            return JSON.parse(cached);
        }

        return null;
    }

    async setCachedDocument(
        repositoryId: string,
        documentPath: string,
        document: Document,
        version?: string
    ): Promise<void> {
        const cacheKey = this.buildDocumentCacheKey(repositoryId, documentPath, version);

        await this.documentCache.set(
            cacheKey,
            JSON.stringify(document),
            { ttl: 3600 } // 1 hour
        );

        // Set up cache invalidation
        await this.setupCacheInvalidation(repositoryId, documentPath);
    }

    async invalidateDocumentCache(repositoryId: string, documentPath: string): Promise<void> {
        const pattern = `doc:${repositoryId}:${documentPath}:*`;
        await this.documentCache.deletePattern(pattern);

        // Invalidate related caches
        await this.invalidateSearchCache(repositoryId);
        await this.invalidateRenderCache(repositoryId, documentPath);
    }
}
```

### Database Optimization
```sql
-- Optimized indexes for performance
CREATE INDEX CONCURRENTLY idx_documents_repo_path
ON documents(repository_id, path)
INCLUDE (title, content_type, updated_at);

CREATE INDEX CONCURRENTLY idx_commits_repo_branch
ON commits(repository_id, branch, created_at DESC);

CREATE INDEX CONCURRENTLY idx_git_objects_repo_hash
ON git_objects(repository_id, content_hash)
INCLUDE (object_type, storage_path);

-- Partitioning for large tables
CREATE TABLE audit_events (
    id UUID,
    timestamp TIMESTAMP WITH TIME ZONE,
    user_id UUID,
    action VARCHAR(100),
    resource_id UUID,
    details JSONB
) PARTITION BY RANGE (timestamp);

-- Create monthly partitions
CREATE TABLE audit_events_2024_01 PARTITION OF audit_events
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

## Monitoring & Observability

### Application Metrics
```typescript
class MetricsCollector {
    private prometheus: PrometheusRegistry;

    constructor() {
        this.setupMetrics();
    }

    private setupMetrics(): void {
        // Business metrics
        this.prometheus.register(new Counter({
            name: 'documents_created_total',
            help: 'Total number of documents created',
            labelNames: ['repository_id', 'content_type']
        }));

        this.prometheus.register(new Histogram({
            name: 'document_search_duration_seconds',
            help: 'Time spent on document searches',
            labelNames: ['repository_id', 'search_type'],
            buckets: [0.1, 0.5, 1.0, 2.0, 5.0]
        }));

        this.prometheus.register(new Gauge({
            name: 'active_collaborators',
            help: 'Number of active collaborators per document',
            labelNames: ['repository_id', 'document_id']
        }));
    }

    recordDocumentCreation(repositoryId: string, contentType: string): void {
        this.prometheus.get('documents_created_total')
            .labels(repositoryId, contentType)
            .inc();
    }

    recordSearchDuration(repositoryId: string, searchType: string, duration: number): void {
        this.prometheus.get('document_search_duration_seconds')
            .labels(repositoryId, searchType)
            .observe(duration);
    }
}
```

### Health Monitoring
```typescript
class HealthMonitor {
    async checkSystemHealth(): Promise<HealthStatus> {
        const checks = await Promise.allSettled([
            this.checkDatabaseHealth(),
            this.checkFileStorageHealth(),
            this.checkSearchEngineHealth(),
            this.checkCacheHealth(),
            this.checkExternalIntegrationsHealth()
        ]);

        const results = checks.map((check, index) => ({
            component: ['database', 'storage', 'search', 'cache', 'integrations'][index],
            status: check.status === 'fulfilled' ? check.value.status : 'unhealthy',
            details: check.status === 'fulfilled' ? check.value.details : check.reason
        }));

        const overallStatus = results.every(r => r.status === 'healthy')
            ? 'healthy'
            : 'degraded';

        return {
            status: overallStatus,
            timestamp: new Date(),
            components: results
        };
    }
}
```

## Security & Compliance

### Data Encryption
```typescript
class EncryptionService {
    private keyManagement: KeyManagementService;

    async encryptDocument(content: string, documentId: string): Promise<EncryptedContent> {
        // Get document-specific encryption key
        const key = await this.keyManagement.getDocumentKey(documentId);

        // Encrypt content
        const encrypted = await this.encrypt(content, key);

        return {
            encryptedContent: encrypted.data,
            iv: encrypted.iv,
            keyId: key.id,
            algorithm: 'AES-256-GCM'
        };
    }

    async decryptDocument(
        encryptedContent: EncryptedContent,
        documentId: string
    ): Promise<string> {
        // Get encryption key
        const key = await this.keyManagement.getDocumentKey(documentId, encryptedContent.keyId);

        // Decrypt content
        return await this.decrypt(encryptedContent, key);
    }
}
```

This comprehensive system design demonstrates enterprise-level thinking about version control for documentation, with advanced features like real-time collaboration, sophisticated access control, and scalable architecture patterns suitable for large organizations.