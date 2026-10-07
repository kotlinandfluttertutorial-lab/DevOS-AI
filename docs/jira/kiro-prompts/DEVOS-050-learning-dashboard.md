# Kiro Prompt — DEVOS-050: Learning Dashboard + Course + Lesson + Quiz

**Jira:** DEVOS-050 / DEVOS-051 / DEVOS-052 / DEVOS-053 / DEVOS-054  
**Epic:** DEVOS-E08  
**Figma:** FIGMA-28 / FIGMA-29 / FIGMA-30 / FIGMA-31  
**Kiro Spec:** `.kiro/specs/learning/core.md`  
**AI-SDLC Phase:** IMPLEMENT

---

## Prompt

You are implementing the complete Learning system for DevOS AI: dashboard, course details, lessons, quizzes, and the repository connection recommendation engine.

**Existing files to read first:**
- `.kiro/specs/learning/core.md`
- `docs/figma/screen-inventory.md` — Screens 28–31
- `docs/figma/component-inventory.md` — DevOSLessonCard, DevOSQuizOption

**Module:** `feature/feature-learning/` + `data/data-learning/`

**Architecture Rules:**
- Maintain Presentation → Domain → Data dependency direction.
- Hilt is the only DI mechanism — no manual service locators.
- ViewModels expose StateFlow<UiState> — never raw mutable state to Compose.
- API keys / tokens loaded from EncryptedSharedPreferences — never BuildConfig.
- Navigation events via SharedFlow — ViewModel must not import NavController.
- Learning data (courses, lessons, progress) must be cached in Room — never re-fetched on every screen entry.
- AI streaming in lesson recommendations must use Flow<StreamChunk> — never block UI waiting for full response.

**What to implement:**

### 1. LearningDashboardScreen (FIGMA-28)

UiState:
```
LearningUiState:
  Loading
  Success(
    currentCourse: CourseProgress?,
    dailyGoalMinutes: Int,
    todayMinutes: Int,
    streak: Int,
    recommendations: List<LearningTopic>,
    recentLessons: List<LessonSummary>,
    quizAverage: Float,
    repoConnections: List<RepoLearningConnection>,
  )
  Empty  (no courses enrolled)
  Error
```

Layout:
```
TopBar: "Learn"

LazyColumn:
  // Continue learning card (if enrolled)
  ContinueLearningCard(course, progress, onContinue)

  // Daily goal ring
  DailyGoalCard(todayMinutes, goalMinutes, streak)

  // Repository connection banner
  if (repoConnections.isNotEmpty()) {
    RepoConnectionBanner(connection, onExplore)
  }

  // Recommended topics
  SectionHeader("Recommended for You")
  LazyRow { items(recommendations) { LearningTopicCard(it) } }

  // Recent lessons
  SectionHeader("Recent Lessons")
  items(recentLessons) { LessonSummaryItem(it) }

  // Quiz score summary
  QuizScoreCard(average)
```

### 2. Repository Connection Banner

```kotlin
@Composable
fun RepoConnectionBanner(connection: RepoLearningConnection, onExplore: () -> Unit) {
    DevOSCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onExplore,
    ) {
        Row(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
            Icon(Icons.Outlined.School, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(MaterialTheme.spacing.sm))
            Column(Modifier.weight(1f)) {
                Text("You're learning ${connection.topic}",
                    style = MaterialTheme.typography.titleSmall)
                Text("DevOS found ${connection.topic} in ${connection.repoName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DevOSButton("Explore", onClick = onExplore, style = DevOSButtonStyle.Tertiary)
        }
    }
}
```

### 2. CourseDetailsScreen (DEVOS-051, FIGMA-29)

Route: `COURSE_DETAILS/{courseId}`

UiState:
```kotlin
sealed interface CourseDetailsUiState {
    data object Loading : CourseDetailsUiState
    data class Success(
        val course: Course,
        val lessons: List<LessonSummary>,
        val repoConnection: RepoLearningConnection?,
        val userProgress: CourseProgress?,
    ) : CourseDetailsUiState
    data object Empty : CourseDetailsUiState
    data class Error(val message: String, val retryable: Boolean) : CourseDetailsUiState
}
```

ViewModel: `CourseDetailsViewModel`
- `fun loadCourse(courseId: String)`
- `fun startCourse()` — enrolls user, navigates to first lesson via SharedFlow event
- `fun continueCourse()` — navigates to last incomplete lesson

Layout:
```kotlin
Scaffold(
  topBar = { DevOSTopBar(title = course.title, onBack = onBack) }
) { padding ->
  LazyColumn(Modifier.padding(padding)) {
    item { // Hero card
      DevOSCard(Modifier.padding(MaterialTheme.spacing.base)) {
        Column(Modifier.padding(MaterialTheme.spacing.cardPadding)) {
          Text(course.description, style = MaterialTheme.typography.bodyMedium)
          Spacer(Modifier.height(MaterialTheme.spacing.sm))
          Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            DevOSStatusBadge(course.difficulty.label, course.difficulty.color)
            DevOSStatusBadge("${course.durationMinutes} min", MaterialTheme.colorScheme.secondary)
            DevOSStatusBadge("${course.enrolledCount} enrolled", MaterialTheme.colorScheme.tertiary)
          }
          if (userProgress != null) {
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            LinearProgressIndicator(progress = userProgress.fraction, Modifier.fillMaxWidth())
            Text("${userProgress.completedLessons}/${course.totalLessons} lessons",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          repoConnection?.let { conn ->
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            SuggestionChip(
              onClick = {},
              label = { Text("Used in ${conn.repoName}", style = MaterialTheme.typography.labelSmall) },
              icon = { Icon(Icons.Outlined.AccountTree, contentDescription = null) }
            )
          }
        }
      }
    }
    item { SectionHeader("Lessons") }
    itemsIndexed(lessons) { index, lesson ->
      LessonListItem(
        index = index + 1,
        title = lesson.title,
        durationMinutes = lesson.durationMinutes,
        isCompleted = lesson.isCompleted,
        onClick = { onNavigateToLesson(lesson.id) }
      )
    }
    item {
      DevOSButton(
        text = if (userProgress == null) "Start Course" else "Continue",
        onClick = { if (userProgress == null) onStart() else onContinue() },
        modifier = Modifier
          .fillMaxWidth()
          .padding(MaterialTheme.spacing.base),
      )
    }
  }
}
```

Acceptance Criteria (DEVOS-051):
- AC1: Course title, description, and difficulty badge visible in hero card
- AC2: Progress bar shown when user has started the course; hidden when not enrolled
- AC3: Lesson list shows all lessons with completion checkmarks for completed ones
- AC4: Repo connection chip shown when course topic detected in active repository
- AC5: "Start Course" button navigates to first lesson; "Continue" navigates to last incomplete lesson

### 3. LessonScreen (FIGMA-30)

```kotlin
@Composable
fun LessonScreen(
    uiState: LessonUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTryInRepo: (CodeExample) -> Unit,
) {
    Scaffold(
        topBar = { LessonTopBar(current = lesson.order, total = course.lessons.size) },
        bottomBar = {
            LessonNavigationBar(
                canGoPrevious = lesson.order > 1,
                canGoNext = true,
                onPrevious = onPrevious,
                onNext = onNext,
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            item {
                Text(lesson.title, style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(MaterialTheme.spacing.base))
            }
            item {
                DevOSMarkdownText(
                    markdown = lesson.content,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.base),
                    onCodeClick = { code, language ->
                        // Opens code in inline expanded view
                    }
                )
            }
            items(lesson.codeExamples) { example ->
                CodeExampleCard(example, onTryInRepo = { onTryInRepo(example) })
            }
        }
    }
}
```

### 4. QuizScreen (FIGMA-31)

States: `Active(question, selectedOption) | Reviewing(question, selectedOption, correct) | Complete(score, total)`

```kotlin
@Composable
fun QuizScreen(...) {
    Scaffold(topBar = { QuizTopBar(current = questionIndex + 1, total = quiz.questions.size) }) { padding ->
        Column(Modifier.padding(padding).padding(MaterialTheme.spacing.base)) {
            Text(question.text, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(MaterialTheme.spacing.xl))

            question.options.forEachIndexed { index, option ->
                DevOSQuizOption(
                    text = option,
                    state = when {
                        isReviewing && index == question.correctIndex -> QuizOptionState.Correct
                        isReviewing && index == selectedOption -> QuizOptionState.Incorrect
                        index == selectedOption -> QuizOptionState.Selected
                        else -> QuizOptionState.Default
                    },
                    onClick = { if (!isReviewing) onSelectOption(index) }
                )
                Spacer(Modifier.height(MaterialTheme.spacing.sm))
            }

            if (isReviewing) {
                Spacer(Modifier.height(MaterialTheme.spacing.xl))
                ExplanationCard(question.explanation)
                DevOSButton("Next Question", onClick = onNext, modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.weight(1f))
                DevOSButton("Submit", onClick = onSubmit,
                    enabled = selectedOption != null,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
```

### Learning Context Injection into AI Chat (DEVOS-054)

The recommendation engine must also inject active learning context into AI chat responses.

**LearningContextInjector UseCase:**
```kotlin
class InjectLearningContextUseCase @Inject constructor(
    private val learningRepository: LearningRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(userId: String, repoId: String): LearningContext =
        withContext(dispatcher) {
            val recommendations = learningRepository.getRecommendations(userId, repoId)
            val activeCourse = learningRepository.getActiveCourse(userId)
            LearningContext(
                recommendations = recommendations.take(3),
                activeTopic = activeCourse?.topic,
                repoPatterns = recommendations.map { it.detectedPattern },
            )
        }
}
```

The `AIRepository.chat()` call in `AIChatViewModel` should merge this context into the system prompt when the user's message contains learning-related intent (keywords: "learn", "understand", "improve", "what should I", "course", "tutorial").

Additional AC (DEVOS-054):
- AC6: Asking AI "what should I learn next?" returns answer that references patterns detected in the active repository
- AC7: Learning recommendations update within 30 seconds of switching active repository

**Tests:**
- `LearningViewModelTest`: loads dashboard, repository connection detected
- `QuizViewModelTest`: option selection, correct/incorrect scoring, final score calculation
- `LessonScreenTest`: content renders, next/previous navigation, code example shows
- `RecommendationEngineTest`: repo using RAG patterns → RAG course recommended

**Acceptance Criteria:**
- AC1: Dashboard shows current course progress correctly
- AC2: Daily goal ring accurate (today minutes vs goal)
- AC3: Repo connection banner shows for active repo with recognized patterns
- AC4: Lesson content renders markdown including code blocks
- AC5: Code examples from real repos show DevOSCodeBlock
- AC6: "Try in repo" action navigates to CodeViewerScreen
- AC7: Quiz scores correct answer, shows explanation
- AC8: Quiz final score saved and reflected in dashboard average
- AC9: Streak updates correctly day by day
- AC10 (DEVOS-051): Course title, description, and difficulty badge visible in hero card
- AC11 (DEVOS-051): Progress bar shown when user has started the course; hidden when not enrolled
- AC12 (DEVOS-051): Lesson list shows all lessons with completion checkmarks for completed ones
- AC13 (DEVOS-051): Repo connection chip shown when course topic detected in active repository
- AC14 (DEVOS-051): "Start Course" button navigates to first lesson; "Continue" navigates to last incomplete lesson
- AC15 (DEVOS-054): Asking AI "what should I learn next?" returns answer referencing patterns in active repository
- AC16 (DEVOS-054): Learning recommendations update within 30 seconds of switching active repository
