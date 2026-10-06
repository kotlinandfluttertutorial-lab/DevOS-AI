---
name: write-tests
description: Write unit tests, ViewModel tests, Repository tests, and Compose UI tests for DevOS AI features
---

# Skill: Write Tests for DevOS AI

## Test Stack

| Type | Framework | Location |
|------|-----------|----------|
| Unit (UseCase, Domain) | JUnit 5 + MockK + Turbine | `<module>/src/test/` |
| ViewModel | JUnit 5 + MockK + Turbine + Coroutines Test | `feature-*/src/test/` |
| Repository (integration) | Room in-memory + JUnit 5 | `data-*/src/test/` |
| Compose UI | ComposeTestRule + Espresso | `feature-*/src/androidTest/` |
| Navigation | TestNavHostController | `app/src/androidTest/` |

## ViewModel Test Pattern

```kotlin
@ExtendWith(InstantExecutorExtension::class)
class AIChatViewModelTest {

    private val sendMessageUseCase: SendMessageUseCase = mockk()
    private lateinit var viewModel: AIChatViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AIChatViewModel(sendMessageUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() {
        assertThat(viewModel.uiState.value).isInstanceOf(AIChatUiState.Loading::class.java)
    }

    @Test
    fun `sendMessage emits streaming then success`() = runTest {
        val fakeChunks = listOf(
            ChatChunk(content = "Hello", isComplete = false),
            ChatChunk(content = " world", isComplete = true),
        )
        every { sendMessageUseCase(any(), any()) } returns fakeChunks.asFlow()

        viewModel.uiState.test {
            // skip loading
            awaitItem()
            viewModel.sendMessage("test")
            val streamingState = awaitItem() as AIChatUiState.Success
            assertThat(streamingState.isStreaming).isTrue()
            val finalState = awaitItem() as AIChatUiState.Success
            assertThat(finalState.isStreaming).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendMessage emits error when use case throws`() = runTest {
        every { sendMessageUseCase(any(), any()) } returns flow { throw IOException("Network error") }

        viewModel.uiState.test {
            awaitItem() // loading
            viewModel.sendMessage("test")
            val errorState = awaitItem() as AIChatUiState.Error
            assertThat(errorState.message).contains("Network error")
            assertThat(errorState.retryable).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

## UseCase Test Pattern

```kotlin
class SendMessageUseCaseTest {

    private val aiRepository: AIRepository = mockk()
    private val ragRepository: RAGRepository = mockk()
    private val memoryRepository: MemoryRepository = mockk()
    private val useCase = SendMessageUseCase(aiRepository, ragRepository, memoryRepository)

    @Test
    fun `invoke retrieves chunks and streams response`() = runTest {
        val chunks = listOf(CodeChunk("AuthViewModel.kt", 10, 30, "fun login()..."))
        coEvery { ragRepository.retrieve(any(), any(), any()) } returns chunks
        coEvery { memoryRepository.getRelevantMemory(any()) } returns emptyList()
        every { aiRepository.streamChat(any()) } returns flowOf(
            ChatMessage(id = "1", role = MessageRole.AI, content = "Auth uses AuthViewModel")
        )

        val results = useCase("How does auth work?").toList()

        assertThat(results).hasSize(1)
        assertThat(results[0].content).contains("AuthViewModel")
        coVerify { ragRepository.retrieve("How does auth work?", any(), 10) }
    }
}
```

## Room Integration Test Pattern

```kotlin
@RunWith(AndroidJUnit4::class)
class MessageDaoTest {

    private lateinit var db: DevOSDatabase
    private lateinit var dao: MessageDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DevOSDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.messageDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `insert and retrieve messages`() = runTest {
        val message = MessageEntity(id = "1", sessionId = "s1", role = "user", content = "Hello")
        dao.insert(message)
        val messages = dao.getBySession("s1").first()
        assertThat(messages).hasSize(1)
        assertThat(messages[0].content).isEqualTo("Hello")
    }
}
```

## Compose UI Test Pattern

```kotlin
class AIChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `loading state shows shimmer`() {
        composeTestRule.setContent {
            DevOSTheme {
                AIChatScreen(
                    uiState = AIChatUiState.Loading,
                    onSendMessage = {},
                    onNavigateToAnswer = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("DevOSLoadingState").assertIsDisplayed()
    }

    @Test
    fun `success state shows message list`() {
        val messages = listOf(
            ChatMessage("1", MessageRole.USER, "How does auth work?"),
            ChatMessage("2", MessageRole.AI, "Auth uses AuthViewModel..."),
        )
        composeTestRule.setContent {
            DevOSTheme {
                AIChatScreen(
                    uiState = AIChatUiState.Success(messages, isStreaming = false),
                    onSendMessage = {},
                    onNavigateToAnswer = {},
                )
            }
        }
        composeTestRule.onNodeWithText("How does auth work?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auth uses AuthViewModel...").assertIsDisplayed()
    }

    @Test
    fun `send button fires callback`() {
        var sentMessage = ""
        composeTestRule.setContent {
            DevOSTheme {
                AIChatScreen(
                    uiState = AIChatUiState.Success(emptyList(), false),
                    onSendMessage = { sentMessage = it },
                    onNavigateToAnswer = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("ChatInput").performTextInput("Hello")
        composeTestRule.onNodeWithTag("SendButton").performClick()
        assertThat(sentMessage).isEqualTo("Hello")
    }

    @Test
    fun `error state shows retry button`() {
        composeTestRule.setContent {
            DevOSTheme {
                AIChatScreen(
                    uiState = AIChatUiState.Error("Network error", retryable = true),
                    onSendMessage = {},
                    onNavigateToAnswer = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
```

## AI-Specific Test Patterns

### Retrieval Precision Test
```kotlin
@Test
fun `rag retrieval returns relevant chunks for auth query`() = runTest {
    // Seed the vector store with known chunks
    ragRepository.indexChunks(testAuthChunks)

    val results = ragRepository.retrieve("How does authentication work?", AIContext.Global, topK = 5)

    val relevantFiles = results.map { it.filePath }
    assertThat(relevantFiles).containsAnyOf("AuthViewModel.kt", "AuthRepository.kt", "LoginUseCase.kt")
    assertThat(results.first().relevance).isGreaterThan(0.7f)
}
```

### Grounding Test
```kotlin
@Test
fun `ai answer cites actual code from repository`() = runTest {
    val answer = sendMessageUseCase("Explain the login flow").first()

    assertThat(answer.sources).isNotEmpty()
    answer.sources.forEach { source ->
        assertThat(fileRepository.exists(source.filePath)).isTrue()
        assertThat(source.lineStart).isLessThan(source.lineEnd)
    }
}
```

## Test Naming Convention
```
`<what it tests> <condition> <expected behavior>`

Examples:
`initial state is Loading`
`sendMessage emits streaming then success`
`error state shows retry button when retryable is true`
`rag retrieval returns relevant chunks for auth query`
```
