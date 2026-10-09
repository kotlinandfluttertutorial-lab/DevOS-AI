package com.devos.ai.feature.learning.model

/**
 * Stub domain models for the Learning feature.
 * TODO(DEVOS-054): replace with domain module interfaces once data-learning is implemented.
 */

/** Status of a lesson in a course. */
enum class LessonStatus { COMPLETED, CURRENT, LOCKED }

/** Icon category used to pick the background color of a recommendation. */
enum class IconCategory { ARCHITECTURE, TESTING, AI }

/** Progress summary for a course displayed in the dashboard continue-learning card. */
data class CourseProgress(
    val courseId: String,
    val title: String,
    val subtitle: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val estimatedMinutesLeft: Int,
    val repoConnection: String?,
) {
    val fraction: Float get() = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons
}

/** A recommended course shown in the dashboard list. */
data class CourseRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconCategory: IconCategory,
    val isNew: Boolean,
)

/** A single quiz score row shown in the dashboard. */
data class QuizScore(
    val quizTitle: String,
    val score: Int,
    val total: Int,
)

/** Summary of a lesson within a course (for the lesson list). */
data class LessonSummary(
    val id: String,
    val order: Int,
    val title: String,
    val durationMinutes: Int,
    val status: LessonStatus,
)

/** Full course model loaded for the Course Details screen. */
data class Course(
    val id: String,
    val title: String,
    val description: String,
    val language: String,
    val totalLessons: Int,
    val durationMinutes: Int,
    val completedLessons: Int,
    val tags: List<String>,
    val lessons: List<LessonSummary>,
    val repoConnection: String?,
) {
    val fraction: Float get() = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons
}

/** A code example attached to a lesson. */
data class CodeExample(
    val id: String,
    val code: String,
    val language: String,
    val description: String,
)

/** Full lesson content loaded for the Lesson screen. */
data class LessonContent(
    val id: String,
    val courseId: String,
    val order: Int,
    val title: String,
    val totalInCourse: Int,
    val markdownContent: String,
    val codeExamples: List<CodeExample>,
)

/** A single quiz question. */
data class QuizQuestion(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

/** A full quiz with a list of questions. */
data class Quiz(
    val id: String,
    val courseId: String,
    val title: String,
    val questions: List<QuizQuestion>,
)
