# Spec: Learning System — Core

**Jira:** DEVOS-050 / DEVOS-051 / DEVOS-052 / DEVOS-053 / DEVOS-054  
**Epic:** DEVOS-E08  
**AI-SDLC Phase:** IMPLEMENT  
**Status:** 🔵 Planned

---

## Goal
Implement the AI learning system: personalized dashboard, courses, lessons, quizzes, and repository-connected recommendations.

## Domain Models

```kotlin
data class LearningProgress(
    val userId: String,
    val enrolledCourses: List<CourseProgress>,
    val completedLessons: Int,
    val totalLessons: Int,
    val streak: Int,               // days
    val dailyGoalMinutes: Int,
    val todayMinutes: Int,
    val quizAverage: Float,
    val recommendedTopics: List<LearningTopic>,
    val repositoryConnections: List<RepoLearningConnection>,
)

data class Course(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: Difficulty,    // BEGINNER, INTERMEDIATE, ADVANCED
    val durationMinutes: Int,
    val tags: List<String>,
    val lessons: List<Lesson>,
    val repositoryTopics: List<String>,  // topics this course covers
)

data class Lesson(
    val id: String,
    val courseId: String,
    val title: String,
    val content: String,          // Markdown with code blocks
    val codeExamples: List<CodeExample>,
    val durationMinutes: Int,
    val order: Int,
)

data class CodeExample(
    val title: String,
    val code: String,
    val language: String,
    val repoId: String?,          // if linked to a real repo example
    val filePath: String?,
    val lineStart: Int?,
)

data class Quiz(
    val id: String,
    val courseId: String,
    val questions: List<Question>,
)

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class RepoLearningConnection(
    val repoId: String,
    val repoName: String,
    val topic: String,           // e.g., "RAG", "Clean Architecture"
    val courseId: String,
    val confidence: Float,       // how confident we are this repo uses this topic
)
```

## Repository Connection Engine

```kotlin
class LearningRecommendationEngine @Inject constructor(
    private val symbolRepository: SymbolRepository,
    private val fileRepository: FileRepository,
    private val courseRepository: LearningRepository,
) {
    suspend fun analyzeRepository(repoId: String): List<RepoLearningConnection> {
        val symbols = symbolRepository.getTopSymbols(repoId, limit = 200)
        val imports = fileRepository.getTopImports(repoId)
        val topics = extractTopics(symbols, imports)
        return courseRepository.getCourses()
            .filter { course -> course.repositoryTopics.any { it in topics } }
            .map { course -> RepoLearningConnection(repoId, ..., course.id, confidence) }
    }
}
```

## Screens

### LearningDashboardScreen (FIGMA-28)
- Continue learning card (current course + progress bar)
- Daily goal ring (today minutes / goal)
- Streak badge
- Repository connection banner: "RAG found in your project → Explore RAG course"
- Recommended topics (horizontal scroll cards)
- Recent lessons (list)
- Quiz score summary

### CourseDetailsScreen (FIGMA-29)
- Course header: title, difficulty badge, duration, tags
- Progress bar
- Lesson list with completion checkmarks
- Repository connection if relevant
- Start / Continue button

### LessonScreen (FIGMA-30)
- Progress bar (lesson N of total)
- Content (DevOSMarkdownText)
- Code examples (DevOSCodeBlock — real repo examples if available)
- "Try in your repo" button when linked to real code
- Next / Previous navigation

### QuizScreen (FIGMA-31)
- Question counter (N / total)
- Question text (bodyLarge)
- 4 option buttons (DevOSButton secondary variant)
- Submit → reveal correct/incorrect
- Explanation shown after answer
- Final score with recommendations

## Acceptance Criteria
- [ ] Dashboard shows correct progress and streak
- [ ] Repository connection detected for a project using RAG
- [ ] Course details load with correct lesson count
- [ ] Lesson renders markdown and code examples
- [ ] Code examples from real repos load correctly
- [ ] Quiz scores saved and reflected in dashboard
- [ ] Daily goal ring updates in real time
- [ ] Recommendations change when switching active repository
- [ ] All screens: Loading/Success/Empty/Error states
