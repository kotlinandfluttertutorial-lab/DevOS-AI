package com.devos.ai.feature.agents.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devos.ai.domain.ai.model.MCPServer
import com.devos.ai.domain.ai.model.MCPTool
import com.devos.ai.domain.ai.repository.MCPRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the MCP Tools screen (DEVOS-039 / DA-51).
 *
 * Loads MCP servers from [MCPRepository], exposes them as [MCPUiState],
 * and handles server selection and tool execution (with destructive confirmation).
 *
 * Navigation events flow via [SharedFlow] — NavController is never imported here.
 */
@HiltViewModel
class MCPViewModel @Inject constructor(
    private val mcpRepository: MCPRepository,
) : ViewModel() {

    private val _uiState  = MutableStateFlow<MCPUiState>(MCPUiState.Loading)
    val uiState: StateFlow<MCPUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<MCPNavEvent>()
    val navEvent: SharedFlow<MCPNavEvent> = _navEvent.asSharedFlow()

    init {
        loadServers()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Selects a server and loads its tools. */
    fun selectServer(serverId: String) {
        val current = _uiState.value as? MCPUiState.Success ?: return
        val server = current.servers.find { it.id == serverId } ?: return
        viewModelScope.launch {
            val tools = mcpRepository.getToolsForServer(serverId)
            _uiState.value = current.copy(
                selectedServer        = server,
                tools                 = tools,
                pendingDestructiveTool = null,
            )
        }
    }

    /** Initiates tool run. Destructive tools set [MCPUiState.Success.pendingDestructiveTool]
     *  to show the confirmation dialog; safe tools emit a navigation event directly. */
    fun runTool(tool: MCPTool) {
        val current = _uiState.value as? MCPUiState.Success ?: return
        if (tool.isDestructive) {
            _uiState.value = current.copy(pendingDestructiveTool = tool)
        } else {
            emitToolExecution(tool)
        }
    }

    /** Called when the user confirms a destructive tool dialog. */
    fun confirmDestructiveTool() {
        val current = _uiState.value as? MCPUiState.Success ?: return
        val tool = current.pendingDestructiveTool ?: return
        _uiState.value = current.copy(pendingDestructiveTool = null)
        emitToolExecution(tool)
    }

    /** Called when the user cancels a destructive tool dialog. */
    fun cancelDestructiveTool() {
        val current = _uiState.value as? MCPUiState.Success ?: return
        _uiState.value = current.copy(pendingDestructiveTool = null)
    }

    /** Navigates back. */
    fun onNavigateBack() {
        viewModelScope.launch { _navEvent.emit(MCPNavEvent.NavigateBack) }
    }

    /** Retries after an error. */
    fun retry() {
        _uiState.value = MCPUiState.Loading
        loadServers()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadServers() {
        viewModelScope.launch {
            try {
                mcpRepository.observeServers()
                    .catch { e ->
                        _uiState.value = MCPUiState.Error(
                            message   = e.message ?: "Failed to load MCP servers",
                            retryable = true,
                        )
                    }
                    .collect { servers ->
                        if (servers.isEmpty()) {
                            _uiState.value = MCPUiState.Empty
                            return@collect
                        }

                        // Auto-select first server and load its tools
                        val firstServer = servers.first()
                        val tools = mcpRepository.getToolsForServer(firstServer.id)
                        _uiState.value = MCPUiState.Success(
                            servers        = servers,
                            selectedServer = firstServer,
                            tools          = tools,
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = MCPUiState.Error(
                    message   = e.message ?: "Failed to load MCP servers",
                    retryable = true,
                )
            }
        }
    }

    private fun emitToolExecution(tool: MCPTool) {
        val current = _uiState.value as? MCPUiState.Success ?: return
        val serverId = current.selectedServer?.id ?: return
        viewModelScope.launch {
            _navEvent.emit(MCPNavEvent.NavigateToToolExecution(serverId, tool.name))
        }
    }
}
