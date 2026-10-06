# Kiro Prompt — DEVOS-031: RAG Pipeline

**Jira:** DEVOS-031  
**Epic:** DEVOS-E04  
**Figma:** N/A (backend)  
**Kiro Spec:** `.kiro/specs/ai-platform/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the RAG (Retrieval-Augmented Generation) pipeline for DevOS AI — the system that grounds AI answers in actual repository code.

**Existing files to read first:**
- `.kiro/specs/ai-platform/core.md` — full RAG architecture
- `docs/architecture/android-architecture.md` — data layer patterns

**Module:** `data/data-ai/`  
**Package:** `com.devos.ai.data.ai.rag`

**What to implement:**

### 1. Domain Interface (in `domain/domain-ai/`)
```kotlin
interface RAGRepository {
    suspend fun indexChunks(repoId: String, chunks: List<CodeChunk>)
    suspend fun retrieve(query: String, context: AIContext, topK: Int = 10): List<CodeChunk>
    suspend fun deleteChunks(repoId: String)
    suspend fun updateChunks(repoId: String, changedFiles: List<String>)
}
```

### 2. Code Chunker
```kotlin
class CodeChunker @Inject constructor() {
    fun chunk(file: CodeFile, content: String): List<CodeChunk> {
        // Strategy: semantic chunking by top-level declarations
        // Fall back to fixed-size (512 token) overlap (64 token) chunks
        // Each chunk: filePath, lineStart, lineEnd, content, language
    }
}
```

Chunking strategy (in order of preference):
1. **Semantic**: chunk by function/class boundaries (use symbol index if available)
2. **Fixed-size**: 512 tokens with 64-token overlap when semantic not available

### 3. Embedding
```kotlin
interface EmbeddingProvider {
    suspend fun embed(text: String): FloatArray
    suspend fun embedBatch(texts: List<String>): List<FloatArray>
}

// Implementations:
// OpenAIEmbeddingProvider (text-embedding-3-small)
// OllamaEmbeddingProvider (nomic-embed-text)
```

### 4. Vector Store (Room-based for on-device)
```kotlin
@Entity(tableName = "code_chunks")
data class CodeChunkEntity(
    @PrimaryKey val id: String,
    val repoId: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val content: String,
    val language: String,
    @TypeConverter val embedding: FloatArray,  // stored as BLOB
)

@Dao
interface CodeChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<CodeChunkEntity>)

    @Query("SELECT * FROM code_chunks WHERE repoId = :repoId")
    suspend fun getByRepo(repoId: String): List<CodeChunkEntity>

    // Cosine similarity computed in-memory (SQLite doesn't support vector ops)
    // For production: consider sqlite-vec extension or remote vector DB
}
```

### 5. Similarity Search
```kotlin
class RAGRepositoryImpl @Inject constructor(
    private val chunkDao: CodeChunkDao,
    private val embeddingProvider: EmbeddingProvider,
) : RAGRepository {

    override suspend fun retrieve(query: String, context: AIContext, topK: Int): List<CodeChunk> =
        withContext(Dispatchers.Default) {
            val queryEmbedding = embeddingProvider.embed(query)
            val allChunks = chunkDao.getByRepo(extractRepoId(context))

            allChunks
                .map { it to cosineSimilarity(queryEmbedding, it.embedding) }
                .sortedByDescending { it.second }
                .take(topK)
                .map { (chunk, score) -> chunk.toDomain().copy(relevance = score) }
        }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val dot = a.zip(b).sumOf { (x, y) -> (x * y).toDouble() }
        val magA = sqrt(a.sumOf { (it * it).toDouble() })
        val magB = sqrt(b.sumOf { (it * it).toDouble() })
        return (dot / (magA * magB)).toFloat()
    }
}
```

### 6. Indexing WorkManager Job
```kotlin
@HiltWorker
class RAGIndexWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val codeChunker: CodeChunker,
    private val ragRepository: RAGRepository,
    private val fileRepository: FileRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repoId = inputData.getString("repoId") ?: return@withContext Result.failure()
        val files = fileRepository.getIndexableFiles(repoId)
        files.chunked(50).forEach { batch ->
            val chunks = batch.flatMap { file ->
                val content = fileRepository.readContent(file)
                codeChunker.chunk(file, content)
            }
            ragRepository.indexChunks(repoId, chunks)
        }
        Result.success()
    }
}
```

**Tests:**
- `CodeChunkerTest`: chunk a 200-line Kotlin file; verify chunks have correct line ranges
- `RAGRepositoryTest`: index 10 chunks; retrieve with query; verify top result is relevant
- `RetrievalPrecisionTest`: seed with auth-related chunks; query "authentication"; top result is auth-related
- `CosineSimilarityTest`: verify similarity computation correctness

**Acceptance Criteria:**
- [ ] Kotlin file chunked into semantically meaningful chunks
- [ ] Chunks embedded and stored in Room
- [ ] Retrieval returns top-10 relevant chunks for auth query
- [ ] Precision >70% for known queries against test repository
- [ ] Incremental update re-indexes only changed files
- [ ] Indexing runs in WorkManager (not blocking UI)
- [ ] Memory usage stays under 200MB during indexing of 10k-file repo
