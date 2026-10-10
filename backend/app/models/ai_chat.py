"""AI Chat request/response schemas."""
from typing import Literal
from pydantic import BaseModel


class AIMessage(BaseModel):
    role: Literal["user", "assistant", "system"]
    content: str


class ChatRequest(BaseModel):
    messages: list[AIMessage]
    model: str = "gpt-4o-mini"
    temperature: float = 0.7
    max_tokens: int = 2048
    stream: bool = False


class ChatResponse(BaseModel):
    id: str
    model: str
    message: AIMessage
    finish_reason: str = "stop"


class ChatStreamChunk(BaseModel):
    id: str
    delta: str
    finish_reason: str | None = None
