package com.devos.ai.feature.learning.course

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.learning.model.Course
import com.devos.ai.feature.learning.model.LessonStatus
import com.devos.ai.feature.learning.model.LessonSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Course Details screen (DEVOS-051).
 *
 * Reads courseId from [SavedStateHandle] and loads stub course data.
 * TODO(DEVOS-054): replace stub data with GetCourseDetailsUseCase.
 */
@HiltViewModel
class CourseDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val courseId: String = checkNotNull(savedStateHandle["courseId"])

    private val _uiState = MutableStateFlow<CourseDetailsUiState>(CourseDetailsUiState.Loading)
    val uiState: StateFlow<CourseDetailsUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<CourseDetailsNavEvent>()
    val navEvent: SharedFlow<CourseDetailsNavEvent> = _navEvent.asSharedFlow()

    init {
        loadCourse()
    }

    /** Loads course data. Call again on retry. */
    fun loadCourse() {
        viewModelScope.launch {
            _uiState.value = CourseDetailsUiState.Loading
            try {
                // TODO(DEVOS-054): call GetCourseDetailsUseCase(courseId)
                _uiState.value = CourseDetailsUiState.Success(stubCourse(courseId))
            } catch (e: Exception) {
                _uiState.value = CourseDetailsUiState.Error(
                    message = e.message ?: "Failed to load course",
                    retryable = true,
                )
            }
        }
    }

    /** Navigate to the first incomplete (CURRENT) lesson, or first lesson if none. */
    fun continueCourse() {
        val state = _uiState.value as? CourseDetailsUiState.Success ?: return
        val course = state.course
        val currentLesson = course.lessons.firstOrNull { it.status == LessonStatus.CURRENT }
            ?: course.lessons.firstOrNull { it.status != LessonStatus.COMPLETED }
            ?: course.lessons.firstOrNull()
            ?: return
        emit(CourseDetailsNavEvent.NavigateToLesson(courseId, currentLesson.id))
    }

    fun navigateToLesson(lessonId: String) {
        emit(CourseDetailsNavEvent.NavigateToLesson(courseId, lessonId))
    }

    fun navigateBack() = emit(CourseDetailsNavEvent.NavigateBack)

    private fun emit(event: CourseDetailsNavEvent) {
        viewModelScope.launch { _navEvent.emit(event) }
    }

    // ── Stub data ──────────────────────────────────────────────────────────────

    private fun stubCourse(id: String) = Course(
        id = id,
        title = "Kotlin Coroutines & Flow",
        description = "Master asynchronous programming with Kotlin Coroutines and Flow. Learn how to write clean, efficient, non-blocking code using modern Kotlin patterns.",
        language = "KOTLIN",
        totalLessons = 6,
        durationMinutes = 45,
        completedLessons = 4,
        tags = listOf("Kotlin", "Intermediate", "In your repo"),
        repoConnection = "DevOS AI",
        lessons = listOf(
            LessonSummary("lesson-1", 1, "Introduction to Coroutines", 8, LessonStatus.COMPLETED),
            LessonSummary("lesson-2", 2, "Suspend Functions", 7, LessonStatus.COMPLETED),
            LessonSummary("lesson-3", 3, "Coroutine Scopes & Contexts", 8, LessonStatus.COMPLETED),
            LessonSummary("lesson-4", 4, "Flow Basics", 9, LessonStatus.COMPLETED),
            LessonSummary("lesson-5", 5, "Flow Operators", 8, LessonStatus.CURRENT),
            LessonSummary("lesson-6", 6, "StateFlow & SharedFlow", 5, LessonStatus.LOCKED),
        ),
    )
}
