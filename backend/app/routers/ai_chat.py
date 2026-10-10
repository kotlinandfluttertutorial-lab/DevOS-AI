"""
AI Chat router.

POST /v1/chat          — single-shot completion
POST /v1/chat/stream   — SSE streaming completion
"""
import json

from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.models.ai_chat import ChatRequest, ChatResponse
from app.services.ai_service import ai_service

router = APIRouter()


@router.post("/chat", response_model=ChatResponse, tags=["ai"])
async def chat(request: ChatRequest) -> ChatResponse:
    """Non-streaming chat completion."""
    return await ai_service.chat(request)


@router.post("/chat/stream", tags=["ai"])
async def chat_stream(request: ChatRequest) -> StreamingResponse:
    """
    Streaming chat completion via Server-Sent Events (SSE).
    The Android client consumes this with OkHttp EventSourceListener.
    """

    async def event_generator():
        async for chunk in ai_service.chat_stream(request):
            data = chunk.model_dump_json()
            yield f"data: {data}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )
