"""AI Chat, RAG, and Agent request/response schemas."""
from datetime import datetime
from enum import Enum
from typing import Literal, Optional

from pydantic import BaseModel


class AIProvider(str, Enum):
    OPENAI = "openai"
    ANTHROPIC = "anthropic"
    GEMINI = "gemini"
    OLLAMA = "ollama"


class MessageRole(str, Enum):
    USER = "user"
    ASSISTANT = "assistant"
    SYSTEM = "system"


class AIMessage(BaseModel):
    role: MessageRole
    content: str


class ChatRequest(BaseModel):
    messages: list[AIMessage]
    provider: AIProvider = AIProvider.OPENAI
    model: Optional[str] = None  # uses provider default if None
    repository_id: Optional[str] = None  # for RAG context
    max_tokens: int = 2048
    temperature: float = 0.7
    stream: bool = False


class ChatResponse(BaseModel):
    id: str
    content: str
    provider: AIProvider
    model: str
    tokens_used: int
    created_at: datetime


class ChatStreamChunk(BaseModel):
    id: str
    delta: str  # partial content chunk
    is_final: bool = False
    provider: AIProvider


# ---------------------------------------------------------------------------
# RAG models
# ---------------------------------------------------------------------------


class RAGQuery(BaseModel):
    query: str
    repository_id: str
    top_k: int = 5
    include_context: bool = True


class RAGResult(BaseModel):
    query: str
    answer: str
    sources: list[dict]  # [{file_path, line_start, line_end, snippet}]
    provider: AIProvider
    tokens_used: int


# ---------------------------------------------------------------------------
# Agent models
# ---------------------------------------------------------------------------


class AgentTool(BaseModel):
    name: str
    description: str
    parameters: dict  # JSON schema


class AgentExecuteRequest(BaseModel):
    prompt: str
    tools: list[AgentTool] = []
    repository_id: Optional[str] = None
    provider: AIProvider = AIProvider.OPENAI
    max_iterations: int = 10


class AgentStep(BaseModel):
    step_number: int
    thought: str
    action: Optional[str] = None
    action_input: Optional[dict] = None
    observation: Optional[str] = None


class AgentExecuteResponse(BaseModel):
    id: str
    status: Literal["completed", "running", "failed"]
    steps: list[AgentStep]
    final_answer: Optional[str] = None
    error: Optional[str] = None


# ---------------------------------------------------------------------------
# Provider info model
# ---------------------------------------------------------------------------


class ProviderInfo(BaseModel):
    name: AIProvider
    has_key: bool
    default_model: str
    available: bool
