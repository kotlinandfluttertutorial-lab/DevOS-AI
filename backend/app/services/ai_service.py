"""
Placeholder AI service.

In production this layer forwards requests to OpenAI, Anthropic, or Gemini
depending on the model selected. SSE streaming is relayed back to the client
via an async generator so the Android app can consume it token-by-token.
"""
import asyncio
import uuid
from collections.abc import AsyncGenerator

from app.models.ai_chat import AIMessage, ChatRequest, ChatResponse, ChatStreamChunk


class AIService:
    """
    Thin adapter over AI provider APIs.
    Replace the stub bodies with real SDK calls when API keys are configured.
    """

    async def chat(self, request: ChatRequest) -> ChatResponse:
        """Single-shot (non-streaming) chat completion."""
        # TODO: replace stub with real provider call
        return ChatResponse(
            id=str(uuid.uuid4()),
            model=request.model,
            message=AIMessage(
                role="assistant",
                content="[AI service not yet connected — configure OPENAI_API_KEY]",
            ),
            finish_reason="stop",
        )

    async def chat_stream(
        self, request: ChatRequest
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        """
        Streaming chat completion.
        Yields ChatStreamChunk objects that the router converts to SSE events.
        """
        # TODO: replace stub with real provider streaming call
        stub_tokens = ["[", "AI", " stream", " stub", "]"]
        chunk_id = str(uuid.uuid4())
        for token in stub_tokens:
            await asyncio.sleep(0.05)  # simulate network latency
            yield ChatStreamChunk(id=chunk_id, delta=token)
        yield ChatStreamChunk(id=chunk_id, delta="", finish_reason="stop")


ai_service = AIService()
