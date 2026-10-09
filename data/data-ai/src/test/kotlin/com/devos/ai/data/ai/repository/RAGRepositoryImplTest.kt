package com.devos.ai.data.ai.repository

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import com.devos.ai.core.database.dao.ChunkDao
import com.devos.ai.core.database.dao.FileDao
import com.devos.ai.core.database.entity.CodeChunkEntity
import com.devos.ai.data.ai.rag.VectorStore
import com.devos.ai.domain.ai.model.AIContext
import com.devos.ai.domain.ai.repository.CodeChunk
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [RAGRepositoryImpl].
 *
 * All DAO calls are mocked — no Room runtime needed.
 *
 * Covers:
 * - [RAGRepositoryImpl.indexChunks] builds vocabulary, embeds chunks, persists
 * - [RAGRepositoryImpl.retrieve] returns ranked results above minRelevance
 * - [RAGRepositoryImpl.deleteChunks] calls DAO
 * - Empty corpus and empty query edge cases
 * - Vocabulary survives round-trip and retrieval finds relevant chunks
 */
class RAGRepositoryImplTest {

    private lateinit var chunkDao: ChunkDao
    private lateinit var fileDao: FileDao
    private lateinit var impl: RAGRepositoryImpl

    private val repoId = "test-repo"

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        chunkDao = mockk(relaxed = true)
        fileDao  = mockk(relaxed = true)
        impl     = RAGRepositoryImpl(chunkDao, fileDao)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun codeChunk(
        id: String,
        content: String,
        filePath: String = "Foo.kt",
        lineStart: Int = 1,
    ) = CodeChunk(
        id        = id,
        repoId    = repoId,
        filePath  = filePath,
        lineStart = lineStart,
        lineEnd   = lineStart + 10,
        content   = content,
        language  = "kotlin",
    )

    // ── indexChunks ───────────────────────────────────────────────────────────

    @Test
    fun `indexChunks inserts embedded rows into chunkDao`() = runTest {
        val chunks = listOf(
            codeChunk("c1", "class UserRepository"),
            codeChunk("c2", "fun fetchUser(id: String)"),
        )

        impl.indexChunks(repoId, chunks)

        // Should insert chunks + 1 vocab row
        coVerify(exactly = 1) {
            chunkDao.insertAll(
                match { rows ->
                    rows.size == 3 &&           // 2 content + 1 vocab
                    rows.count { it.filePath == "__vocab__" } == 1 &&
                    rows.filter { it.filePath != "__vocab__" }
                        .all { it.embeddingJson != null }
                },
            )
        }
    }

    @Test
    fun `indexChunks stores non-null embeddingJson on each content chunk`() = runTest {
        val inserted = mutableListOf<CodeChunkEntity>()
        coEvery { chunkDao.insertAll(any()) } answers {
            inserted.addAll(firstArg())
        }

        impl.indexChunks(repoId, listOf(
            codeChunk("c1", "suspend fun loadData(): Result<String>"),
        ))

        val contentChunks = inserted.filter { it.filePath != "__vocab__" }
        assertThat(contentChunks).isNotEmpty()
        assertThat(contentChunks.all { it.embeddingJson != null }).isEqualTo(true)
    }

    @Test
    fun `indexChunks with empty list inserts only vocab row`() = runTest {
        val inserted = mutableListOf<CodeChunkEntity>()
        coEvery { chunkDao.insertAll(any()) } answers { inserted.addAll(firstArg()) }

        impl.indexChunks(repoId, emptyList())

        // One vocab row
        assertThat(inserted).hasSize(1)
        assertThat(inserted[0].filePath).isEqualTo("__vocab__")
    }

    // ── retrieve ──────────────────────────────────────────────────────────────

    @Test
    fun `retrieve returns empty when no chunks stored`() = runTest {
        coEvery { chunkDao.getEmbeddedByRepo(any()) } returns emptyList()
        coEvery { chunkDao.getById(any()) } returns null

        val context = AIContext.Repository(repoId, "TestRepo")
        val results = impl.retrieve("getUserById", context)
        assertThat(results).isEmpty()
    }

    @Test
    fun `retrieve returns empty for Global context (not supported in MVP)`() = runTest {
        val results = impl.retrieve("anything", AIContext.Global)
        assertThat(results).isEmpty()
    }

    @Test
    fun `retrieve finds relevant chunks after indexing`() = runTest {
        // Index some chunks with real content
        val chunks = listOf(
            codeChunk("auth", "class AuthRepository implements UserAuth"),
            codeChunk("repo", "class RepositoryViewModel loads repositories"),
            codeChunk("chat", "fun sendMessage creates AI chat response"),
        )

        // Capture what gets inserted
        val stored = mutableListOf<CodeChunkEntity>()
        coEvery { chunkDao.insertAll(any()) } answers { stored.addAll(firstArg()) }
        impl.indexChunks(repoId, chunks)

        // Now mock the retrieval DAO calls
        val contentRows = stored.filter { it.filePath != "__vocab__" }
        val vocabRow    = stored.first  { it.filePath == "__vocab__" }

        coEvery { chunkDao.getEmbeddedByRepo(repoId) } returns contentRows
        coEvery { chunkDao.getById("vocab:$repoId") } returns vocabRow

        val context = AIContext.Repository(repoId, "TestRepo")
        val results = impl.retrieve("authentication user repository", context, topK = 3)

        // Should get at least one result
        assertThat(results).isNotEmpty()
        // Top result should be relevant (score > 0)
        assertThat(results[0].relevance).isGreaterThan(0f)
    }

    @Test
    fun `retrieve respects topK limit`() = runTest {
        val chunks = (1..10).map { codeChunk("c$it", "fun method$it() { /* body */ }") }

        val stored = mutableListOf<CodeChunkEntity>()
        coEvery { chunkDao.insertAll(any()) } answers { stored.addAll(firstArg()) }
        impl.indexChunks(repoId, chunks)

        val contentRows = stored.filter { it.filePath != "__vocab__" }
        val vocabRow    = stored.first  { it.filePath == "__vocab__" }
        coEvery { chunkDao.getEmbeddedByRepo(repoId) } returns contentRows
        coEvery { chunkDao.getById("vocab:$repoId") } returns vocabRow

        val results = impl.retrieve("method body", AIContext.Repository(repoId, "R"), topK = 3)
        assertThat(results.size).isEqualTo(minOf(results.size, 3))
    }

    @Test
    fun `retrieve results are sorted by descending relevance`() = runTest {
        val chunks = listOf(
            codeChunk("match1", "class UserAuthenticationService handles login"),
            codeChunk("match2", "fun authenticateUser verifies credentials"),
            codeChunk("nomatch", "data class DatabaseConfig connection pool"),
        )

        val stored = mutableListOf<CodeChunkEntity>()
        coEvery { chunkDao.insertAll(any()) } answers { stored.addAll(firstArg()) }
        impl.indexChunks(repoId, chunks)

        val contentRows = stored.filter { it.filePath != "__vocab__" }
        val vocabRow    = stored.first  { it.filePath == "__vocab__" }
        coEvery { chunkDao.getEmbeddedByRepo(repoId) } returns contentRows
        coEvery { chunkDao.getById("vocab:$repoId") } returns vocabRow

        val results = impl.retrieve("authentication user login", AIContext.Repository(repoId, "R"))
        for (i in 0 until results.size - 1) {
            assertThat(results[i].relevance >= results[i + 1].relevance).isEqualTo(true)
        }
    }

    // ── deleteChunks ──────────────────────────────────────────────────────────

    @Test
    fun `deleteChunks delegates to DAO`() = runTest {
        impl.deleteChunks(repoId)
        coVerify(exactly = 1) { chunkDao.deleteByRepo(repoId) }
    }
}
