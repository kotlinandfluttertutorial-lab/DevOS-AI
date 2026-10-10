"""
Agents router.

POST /v1/agents/execute          — submit an agent execution request
GET  /v1/agents/{agent_id}/status — poll status of a running agent
"""
import uuid
from typing import Any

from fastapi import APIRouter, HTTPException

from app.models.ai_chat import AgentExecuteRequest, AgentExecuteResponse
from app.services.ai_service import ai_service

router = APIRouter()

# In-memory store for agent status (keyed by agent_id).
# In production this would be backed by Redis / the database.
_agent_store: dict[str, AgentExecuteResponse] = {}


@router.post("/agents/execute", response_model=AgentExecuteResponse, tags=["agents"])
async def execute_agent(request: AgentExecuteRequest) -> AgentExecuteResponse:
    """
    Execute an AI agent with optional tool-calling.
    Returns a full AgentExecuteResponse with execution steps and final answer.
    The result is stored in-memory so it can be retrieved via the status endpoint.
    """
    result = await ai_service.execute_agent(request)
    _agent_store[result.id] = result
    return result


@router.get(
    "/agents/{agent_id}/status",
    response_model=AgentExecuteResponse,
    tags=["agents"],
)
async def get_agent_status(agent_id: str) -> AgentExecuteResponse:
    """
    Retrieve the current status of an agent execution by its ID.
    Returns 404 if the agent_id is unknown.
    """
    result = _agent_store.get(agent_id)
    if result is None:
        raise HTTPException(
            status_code=404, detail=f"Agent execution '{agent_id}' not found."
        )
    return result
