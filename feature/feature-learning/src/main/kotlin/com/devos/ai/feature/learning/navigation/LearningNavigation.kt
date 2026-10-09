package com.devos.ai.feature.learning.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devos.ai.feature.learning.course.CourseDetailsNavEvent
import com.devos.ai.feature.learning.course.CourseDetailsScreen
import com.devos.ai.feature.learning.course.CourseDetailsViewModel
import com.devos.ai.feature.learning.dashboard.LearningDashboardNavEvent
import com.devos.ai.feature.learning.dashboard.LearningDashboardScreen
import com.devos.ai.feature.learning.dashboard.LearningDashboardViewModel
import com.devos.ai.feature.learning.lesson.LessonNavEvent
import com.devos.ai.feature.learning.lesson.LessonScreen
import com.devos.ai.feature.learning.lesson.LessonViewModel
import com.devos.ai.feature.learning.quiz.QuizNavEvent
import com.devos.ai.feature.learning.quiz.QuizScreen
import com.devos.ai.feature.learning.quiz.QuizViewModel

// Local route mirrors — feature-learning cannot import :app's DevOSRoutes directly.
// These values MUST stay in sync with DevOSRoutes.kt in :app.
private const val ROUTE_LEARNING_DASHBOARD = "learning_dashboard"
private const val ROUTE_COURSE_DETAILS     = "learn/course/{courseId}"
private const val ROUTE_LESSON             = "learn/course/{courseId}/lesson/{lessonId}"
private const val ROUTE_QUIZ               = "learn/course/{courseId}/quiz/{quizId}"

/**
 * Adds the Learning Dashboard screen to the NavGraph (DEVOS-050).
 *
 * Replaces `composable(DevOSRoutes.LEARNING_DASHBOARD) { PlaceholderScreen(...) }` in DevOSNavGraph.
 */
fun NavGraphBuilder.learningDashboardNavigation(navController: NavController) {
    composable(route = ROUTE_LEARNING_DASHBOARD) {
        val viewModel: LearningDashboardViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is LearningDashboardNavEvent.NavigateToCourse ->
                        navController.navigate("learn/course/${event.courseId}")

                    LearningDashboardNavEvent.NavigateToSearch ->
                        navController.navigate("search?q=")
                }
            }
        }

        LearningDashboardScreen(
            uiState = uiState,
            onContinueCourse = viewModel::onContinueCourse,
            onRecommendationTap = viewModel::onRecommendationTap,
            onSearchTap = viewModel::onSearchTap,
            onRetry = viewModel::loadDashboard,
        )
    }
}

/**
 * Adds the Course Details screen to the NavGraph (DEVOS-051).
 */
fun NavGraphBuilder.courseDetailsNavigation(navController: NavController) {
    composable(
        route = ROUTE_COURSE_DETAILS,
        arguments = listOf(navArgument("courseId") { type = NavType.StringType }),
    ) {
        val viewModel: CourseDetailsViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is CourseDetailsNavEvent.NavigateToLesson ->
                        navController.navigate(
                            "learn/course/${event.courseId}/lesson/${event.lessonId}"
                        )

                    CourseDetailsNavEvent.NavigateBack ->
                        navController.popBackStack()
                }
            }
        }

        CourseDetailsScreen(
            uiState = uiState,
            onContinueCourse = viewModel::continueCourse,
            onLessonTap = viewModel::navigateToLesson,
            onBack = viewModel::navigateBack,
            onRetry = viewModel::loadCourse,
        )
    }
}

/**
 * Adds the Lesson screen to the NavGraph (DEVOS-052).
 */
fun NavGraphBuilder.lessonNavigation(navController: NavController) {
    composable(
        route = ROUTE_LESSON,
        arguments = listOf(
            navArgument("courseId") { type = NavType.StringType },
            navArgument("lessonId") { type = NavType.StringType },
        ),
    ) {
        val viewModel: LessonViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    is LessonNavEvent.NavigateToNextLesson ->
                        navController.navigate(
                            "learn/course/${event.courseId}/lesson/${event.lessonId}"
                        )

                    is LessonNavEvent.NavigateToPreviousLesson -> {
                        navController.popBackStack()
                        navController.navigate(
                            "learn/course/${event.courseId}/lesson/${event.lessonId}"
                        )
                    }

                    is LessonNavEvent.NavigateToCodeViewer ->
                        // TODO: navigate to code viewer when DEVOS-019 is implemented
                        Unit

                    LessonNavEvent.NavigateBack ->
                        navController.popBackStack()
                }
            }
        }

        LessonScreen(
            uiState = uiState,
            onNext = viewModel::onNext,
            onPrevious = viewModel::onPrevious,
            onTryInRepo = viewModel::onTryInRepo,
            onBack = viewModel::navigateBack,
            onRetry = viewModel::loadLesson,
        )
    }
}

/**
 * Adds the Quiz screen to the NavGraph (DEVOS-053).
 */
fun NavGraphBuilder.quizNavigation(navController: NavController) {
    composable(
        route = ROUTE_QUIZ,
        arguments = listOf(
            navArgument("courseId") { type = NavType.StringType },
            navArgument("quizId") { type = NavType.StringType },
        ),
    ) {
        val viewModel: QuizViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.navEvent.collect { event ->
                when (event) {
                    QuizNavEvent.NavigateBack ->
                        navController.popBackStack()

                    is QuizNavEvent.NavigateToCourse ->
                        navController.navigate("learn/course/${event.courseId}") {
                            popUpTo("learn/course/${event.courseId}") { inclusive = false }
                        }
                }
            }
        }

        QuizScreen(
            uiState = uiState,
            onSelectOption = viewModel::selectOption,
            onSubmit = viewModel::submitAnswer,
            onNextQuestion = viewModel::nextQuestion,
            onFinish = viewModel::finishQuiz,
            onBack = viewModel::navigateBack,
            onRetry = viewModel::loadQuiz,
        )
    }
}
