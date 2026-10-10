"""
AI Chat router.

POST /v1/chat           — single-shot chat completion
POST /v1/chat/stream    — SSE streaming chat completion
GET  /v1/providers      — list providers with has_key boolean (NEVER returns key values)
"""
from fastapi import APIRouter
from fastapi.responses import StreamingResponse

from app.config import settings
from app.models.ai_chat import (
    AIProvider,
    ChatRequest,
    ChatResponse,
    ProviderInfo,
)
from app.services.ai_service import ai_service

router = APIRouter()


def _provider_has_key(provider: AIProvider) -> bool:
    """
    Returns True if a non-empty API key is configured for the provider.
    SECURITY: this boolean is all the caller receives — the key value is never exposed.
    """
    if provider == AIProvider.OPENAI:
        return bool(settings.openai_api_key)
    elif provider == AIProvider.ANTHROPIC:
        return bool(settings.anthropic_api_key)
    elif provider == AIProvider.GEMINI:
        return bool(settings.gemini_api_key)
    elif provider == AIProvider.OLLAMA:
        return True  # local, no key needed
    return False


@router.post("/chat", response_model=ChatResponse, tags=["ai"])
async def chat(request: ChatRequest) -> ChatResponse:
    """Non-streaming chat completion. Returns a single ChatResponse."""
    return await ai_service.chat(request)


@router.post("/chat/stream", tags=["ai"])
async def chat_stream(request: ChatRequest) -> StreamingResponse:
    """
    Streaming chat completion via Server-Sent Events (SSE).
    The Android client consumes this with OkHttp EventSourceListener.
    Each chunk is: data: <ChatStreamChunk JSON>\\n\\n
    Stream ends with: data: [DONE]\\n\\n
    """

    async def stream_generator():
        async for chunk in ai_service.chat_stream(request):
            yield f"data: {chunk.model_dump_json()}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(
        stream_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


@router.get("/providers", response_model=list[ProviderInfo], tags=["ai"])
async def list_providers() -> list[ProviderInfo]:
    """
    List all supported AI providers and whether they are configured.
    SECURITY: has_key is a boolean only — the actual key value is NEVER returned.
    """
    from app.services.ai_service import _DEFAULT_MODELS

    providers = []
    for provider in AIProvider:
        has_key = _provider_has_key(provider)
        providers.append(
            ProviderInfo(
                name=provider,
                has_key=has_key,
                default_model=_DEFAULT_MODELS[provider],
                available=has_key,
            )
        )
    return providers
