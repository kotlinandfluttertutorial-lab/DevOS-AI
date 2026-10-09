package com.devos.ai.feature.learning.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.feature.learning.model.CodeExample
import com.devos.ai.feature.learning.model.LessonContent
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
 * ViewModel for the Lesson screen (DEVOS-052).
 *
 * Reads courseId and lessonId from [SavedStateHandle] and loads stub lesson content.
 * TODO(DEVOS-054): replace stub data with GetLessonContentUseCase.
 */
@HiltViewModel
class LessonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val courseId: String = checkNotNull(savedStateHandle["courseId"])
    private val lessonId: String = checkNotNull(savedStateHandle["lessonId"])

    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<LessonNavEvent>()
    val navEvent: SharedFlow<LessonNavEvent> = _navEvent.asSharedFlow()

    init {
        loadLesson()
    }

    fun loadLesson() {
        viewModelScope.launch {
            _uiState.value = LessonUiState.Loading
            try {
                val lesson = stubLesson(courseId, lessonId)
                _uiState.value = LessonUiState.Success(
                    lesson = lesson,
                    canGoPrevious = lesson.order > 1,
                    canGoNext = true,
                )
            } catch (e: Exception) {
                _uiState.value = LessonUiState.Error(
                    message = e.message ?: "Failed to load lesson",
                    retryable = true,
                )
            }
        }
    }

    fun onNext() {
        val state = _uiState.value as? LessonUiState.Success ?: return
        val nextLessonId = "lesson-${state.lesson.order + 1}"
        emit(LessonNavEvent.NavigateToNextLesson(courseId, nextLessonId))
    }

    fun onPrevious() {
        val state = _uiState.value as? LessonUiState.Success ?: return
        if (state.lesson.order <= 1) return
        val prevLessonId = "lesson-${state.lesson.order - 1}"
        emit(LessonNavEvent.NavigateToPreviousLesson(courseId, prevLessonId))
    }

    fun onTryInRepo(example: CodeExample) {
        emit(LessonNavEvent.NavigateToCodeViewer(example.code, example.language))
    }

    fun navigateBack() = emit(LessonNavEvent.NavigateBack)

    private fun emit(event: LessonNavEvent) {
        viewModelScope.launch { _navEvent.emit(event) }
    }

    // ── Stub data ──────────────────────────────────────────────────────────────

    private fun stubLesson(courseId: String, lessonId: String): LessonContent {
        val order = lessonId.removePrefix("lesson-").toIntOrNull() ?: 5
        return LessonContent(
            id = lessonId,
            courseId = courseId,
            order = order,
            title = "Flow Operators",
            totalInCourse = 6,
            markdownContent = """
                ## What are Flow Operators?

                Flow operators let you **transform**, **filter**, and **combine** streams of data in a declarative way.

                They are divided into two categories:
                - **Intermediate operators** — transform the flow without collecting it (e.g. `map`, `filter`, `take`)
                - **Terminal operators** — collect or trigger the flow (e.g. `collect`, `toList`, `first`)

                ### Common Intermediate Operators

                Use `map` to transform each emitted value:

                ```kotlin
                flow { emit(1); emit(2); emit(3) }
                    .map { it * 2 }
                    .collect { println(it) } // 2, 4, 6
                ```

                Use `filter` to drop unwanted values:

                ```kotlin
                flow { emit(1); emit(2); emit(3) }
                    .filter { it % 2 == 0 }
                    .collect { println(it) } // 2
                ```

                ### Combining Flows

                Use `combine` to merge two flows together:

                ```kotlin
                val flow1 = flowOf(1, 2, 3)
                val flow2 = flowOf("a", "b", "c")
                flow1.combine(flow2) { num, letter -> "${'$'}num${'$'}letter" }
                    .collect { println(it) }
                ```

                > **Key insight:** Operators are lazy — they only run when there is a terminal operator downstream.
            """.trimIndent(),
            codeExamples = listOf(
                CodeExample(
                    id = "ex-1",
                    code = """
val repository = flowOf("DevOS AI", "Compose Lib", "SDK Tools")
    .map { name -> name.uppercase() }
    .filter { name -> name.contains("DEVOS") }

viewModelScope.launch {
    repository.collect { println(it) }
}
                    """.trimIndent(),
                    language = "kotlin",
                    description = "Filter and map over a repository name flow",
                ),
            ),
        )
    }
}
