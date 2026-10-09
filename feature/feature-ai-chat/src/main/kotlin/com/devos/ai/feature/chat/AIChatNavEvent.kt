package com.devos.ai.feature.chat

/**
 * One-time navigation events emitted by [AIChatViewModel] via SharedFlow.
 *
 * Collected in [AIChatNavigation] and translated into NavController calls.
 * The ViewModel never imports NavController.
 */
sealed class AIChatNavEvent {
    data class NavigateToAnswer(val answerId: String) : AIChatNavEvent()
    data class NavigateToCode(val filePath: String, val line: Int) : AIChatNavEvent()
    data class NavigateToAgentRun(val runId: String) : AIChatNavEvent()
    data object NavigateBack : AIChatNavEvent()
}
