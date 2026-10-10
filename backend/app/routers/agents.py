"""
Agents router — stub implementation.
POST /v1/agents/execute
"""
import uuid

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class AgentExecuteRequest(BaseModel):
    agent_id: str
    tool: str
    parameters: dict = {}


class AgentExecuteResponse(BaseModel):
    execution_id: str
    status: str
    result: dict | None = None
    error: str | None = None


@router.post("/agents/execute", response_model=AgentExecuteResponse, tags=["agents"])
async def execute_agent(request: AgentExecuteRequest) -> AgentExecuteResponse:
    """
    Execute an agent tool call.
    Stub: returns a queued status. Wire up to real MCP tool execution in a later ticket.
    """
    return AgentExecuteResponse(
        execution_id=str(uuid.uuid4()),
        status="queued",
        result=None,
        error=None,
    )
