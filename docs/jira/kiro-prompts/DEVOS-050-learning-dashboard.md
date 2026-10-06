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

**Tests:**
- `LearningViewModelTest`: loads dashboard, repository connection detected
- `QuizViewModelTest`: option selection, correct/incorrect scoring, final score calculation
- `LessonScreenTest`: content renders, next/previous navigation, code example shows
- `RecommendationEngineTest`: repo using RAG patterns → RAG course recommended

**Acceptance Criteria:**
- [ ] Dashboard shows current course progress correctly
- [ ] Daily goal ring accurate (today minutes vs goal)
- [ ] Repo connection banner shows for active repo with recognized patterns
- [ ] Lesson content renders markdown including code blocks
- [ ] Code examples from real repos show DevOSCodeBlock
- [ ] "Try in repo" action navigates to CodeViewerScreen
- [ ] Quiz scores correct answer, shows explanation
- [ ] Quiz final score saved and reflected in dashboard average
- [ ] Streak updates correctly day by day
