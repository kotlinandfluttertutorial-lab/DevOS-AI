"""
AI Service — routes chat, streaming, RAG, and agent requests to the
correct provider (OpenAI, Anthropic, Gemini, Ollama).

When an API key is not configured the service falls back to a stub
response so local development works without credentials.
"""
import asyncio
import logging
import uuid
from collections.abc import AsyncGenerator
from datetime import datetime, timezone
from typing import Optional

import httpx

from app.config import settings
from app.models.ai_chat import (
    AIMessage,
    AIProvider,
    AgentExecuteRequest,
    AgentExecuteResponse,
    AgentStep,
    ChatRequest,
    ChatResponse,
    ChatStreamChunk,
    MessageRole,
    RAGQuery,
    RAGResult,
)

logger = logging.getLogger("devos.ai_service")

# Provider default models
_DEFAULT_MODELS: dict[AIProvider, str] = {
    AIProvider.OPENAI: "gpt-4o-mini",
    AIProvider.ANTHROPIC: "claude-3-5-haiku-20241022",
    AIProvider.GEMINI: "gemini-1.5-flash",
    AIProvider.OLLAMA: "llama3.2",
}

_STUB_CONTENT = (
    "[Stub] AI provider not configured. "
    "Set OPENAI_API_KEY (or equivalent) in .env to enable real responses."
)


def _resolve_model(provider: AIProvider, model: Optional[str]) -> str:
    """Return the model string, falling back to the provider default."""
    return model or _DEFAULT_MODELS[provider]


def _has_key(provider: AIProvider) -> bool:
    """Return True only if a non-empty key is set for the provider."""
    key_map = {
        AIProvider.OPENAI: settings.openai_api_key,
        AIProvider.ANTHROPIC: settings.anthropic_api_key,
        AIProvider.GEMINI: settings.gemini_api_key,
        AIProvider.OLLAMA: "",  # Ollama is local, no key needed
    }
    # Ollama is always "available" (local)
    if provider == AIProvider.OLLAMA:
        return True
    return bool(key_map.get(provider, ""))


class AIService:
    """
    Thin adapter over AI provider REST APIs.
    Uses httpx.AsyncClient for all outbound calls.
    Falls back to stub responses when API keys are absent.
    """

    # ------------------------------------------------------------------
    # Public interface
    # ------------------------------------------------------------------

    async def chat(self, request: ChatRequest) -> ChatResponse:
        """Single-shot (non-streaming) chat completion."""
        provider = request.provider
        model = _resolve_model(provider, request.model)

        if not _has_key(provider):
            return self._stub_response(provider, model)

        if provider == AIProvider.OPENAI:
            return await self._openai_chat(request, model)
        elif provider == AIProvider.ANTHROPIC:
            return await self._anthropic_chat(request, model)
        elif provider == AIProvider.GEMINI:
            return await self._gemini_chat(request, model)
        elif provider == AIProvider.OLLAMA:
            return await self._ollama_chat(request, model)
        else:
            raise ValueError(f"Unknown provider: {provider}")

    async def chat_stream(
        self, request: ChatRequest
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        """
        Streaming chat completion — yields ChatStreamChunk objects.
        Falls back to a chunked stub when no API key is configured.
        """
        provider = request.provider
        model = _resolve_model(provider, request.model)

        if not _has_key(provider):
            async for chunk in self._stub_stream(provider):
                yield chunk
            return

        if provider == AIProvider.OPENAI:
            async for chunk in self._openai_stream(request, model):
                yield chunk
        elif provider == AIProvider.ANTHROPIC:
            async for chunk in self._anthropic_stream(request, model):
                yield chunk
        elif provider == AIProvider.GEMINI:
            # Gemini streaming: fall back to single-shot then chunk locally
            async for chunk in self._gemini_stream(request, model):
                yield chunk
        elif provider == AIProvider.OLLAMA:
            async for chunk in self._ollama_stream(request, model):
                yield chunk
        else:
            raise ValueError(f"Unknown provider: {provider}")

    async def rag_query(self, query: RAGQuery) -> RAGResult:
        """
        RAG query: retrieve relevant code context then answer.
        Full vector search will be added in a later ticket (DA-129).
        This stub returns a placeholder answer with a sample source hint.
        """
        stub_answer = (
            f"[RAG Stub] Query received for repository '{query.repository_id}'. "
            "Vector index not yet built — full RAG will be enabled after DA-129. "
            f"Your question was: {query.query}"
        )
        sources = [
            {
                "file_path": "src/main/kotlin/com/devos/ai/Main.kt",
                "line_start": 1,
                "line_end": 10,
                "snippet": "// Stub source — vector index not yet available",
            }
        ]
        return RAGResult(
            query=query.query,
            answer=stub_answer,
            sources=sources,
            provider=AIProvider.OPENAI,
            tokens_used=0,
        )

    async def execute_agent(
        self, request: AgentExecuteRequest
    ) -> AgentExecuteResponse:
        """
        Execute an AI agent with tool-calling.
        Full ReAct-loop implementation will follow in a later ticket.
        This stub returns a sample execution trace.
        """
        agent_id = str(uuid.uuid4())
        steps = [
            AgentStep(
                step_number=1,
                thought=f"Received prompt: '{request.prompt}'. Analysing available tools.",
                action=request.tools[0].name if request.tools else None,
                action_input={"query": request.prompt} if request.tools else None,
                observation="[Stub] Tool execution not yet wired up.",
            ),
        ]
        return AgentExecuteResponse(
            id=agent_id,
            status="completed",
            steps=steps,
            final_answer=(
                f"[Agent Stub] Prompt processed. "
                f"Tools available: {[t.name for t in request.tools]}. "
                "Real agent loop will be enabled after MCP integration (DA-130)."
            ),
            error=None,
        )

    # ------------------------------------------------------------------
    # Stub helpers
    # ------------------------------------------------------------------

    def _stub_response(self, provider: AIProvider, model: str) -> ChatResponse:
        return ChatResponse(
            id=str(uuid.uuid4()),
            content=_STUB_CONTENT,
            provider=provider,
            model=model,
            tokens_used=0,
            created_at=datetime.now(timezone.utc),
        )

    async def _stub_stream(
        self, provider: AIProvider
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        chunk_id = str(uuid.uuid4())
        tokens = _STUB_CONTENT.split()
        for i, token in enumerate(tokens):
            await asyncio.sleep(0.02)
            yield ChatStreamChunk(
                id=chunk_id,
                delta=token + (" " if i < len(tokens) - 1 else ""),
                is_final=False,
                provider=provider,
            )
        yield ChatStreamChunk(id=chunk_id, delta="", is_final=True, provider=provider)

    # ------------------------------------------------------------------
    # OpenAI
    # ------------------------------------------------------------------

    async def _openai_chat(self, request: ChatRequest, model: str) -> ChatResponse:
        messages = [{"role": m.role.value, "content": m.content} for m in request.messages]
        payload = {
            "model": model,
            "messages": messages,
            "max_tokens": request.max_tokens,
            "temperature": request.temperature,
        }
        async with httpx.AsyncClient(timeout=60) as client:
            resp = await client.post(
                "https://api.openai.com/v1/chat/completions",
                json=payload,
                headers={
                    "Authorization": f"Bearer {settings.openai_api_key}",
                    "Content-Type": "application/json",
                },
            )
            resp.raise_for_status()
            data = resp.json()

        choice = data["choices"][0]
        return ChatResponse(
            id=data.get("id", str(uuid.uuid4())),
            content=choice["message"]["content"],
            provider=AIProvider.OPENAI,
            model=model,
            tokens_used=data.get("usage", {}).get("total_tokens", 0),
            created_at=datetime.now(timezone.utc),
        )

    async def _openai_stream(
        self, request: ChatRequest, model: str
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        messages = [{"role": m.role.value, "content": m.content} for m in request.messages]
        payload = {
            "model": model,
            "messages": messages,
            "max_tokens": request.max_tokens,
            "temperature": request.temperature,
            "stream": True,
        }
        chunk_id = str(uuid.uuid4())
        async with httpx.AsyncClient(timeout=120) as client:
            async with client.stream(
                "POST",
                "https://api.openai.com/v1/chat/completions",
                json=payload,
                headers={
                    "Authorization": f"Bearer {settings.openai_api_key}",
                    "Content-Type": "application/json",
                },
            ) as resp:
                resp.raise_for_status()
                async for line in resp.aiter_lines():
                    if not line.startswith("data:"):
                        continue
                    raw = line[len("data:"):].strip()
                    if raw == "[DONE]":
                        yield ChatStreamChunk(
                            id=chunk_id, delta="", is_final=True, provider=AIProvider.OPENAI
                        )
                        return
                    try:
                        import json

                        data = json.loads(raw)
                        delta = data["choices"][0]["delta"].get("content", "")
                        if delta:
                            yield ChatStreamChunk(
                                id=data.get("id", chunk_id),
                                delta=delta,
                                is_final=False,
                                provider=AIProvider.OPENAI,
                            )
                    except Exception:
                        continue

    # ------------------------------------------------------------------
    # Anthropic
    # ------------------------------------------------------------------

    async def _anthropic_chat(self, request: ChatRequest, model: str) -> ChatResponse:
        # Separate system prompt from conversation messages
        system_prompt = ""
        messages = []
        for m in request.messages:
            if m.role == MessageRole.SYSTEM:
                system_prompt = m.content
            else:
                messages.append({"role": m.role.value, "content": m.content})

        payload: dict = {
            "model": model,
            "max_tokens": request.max_tokens,
            "messages": messages,
        }
        if system_prompt:
            payload["system"] = system_prompt

        async with httpx.AsyncClient(timeout=60) as client:
            resp = await client.post(
                "https://api.anthropic.com/v1/messages",
                json=payload,
                headers={
                    "x-api-key": settings.anthropic_api_key,
                    "anthropic-version": "2023-06-01",
                    "Content-Type": "application/json",
                },
            )
            resp.raise_for_status()
            data = resp.json()

        content = data["content"][0]["text"] if data.get("content") else ""
        usage = data.get("usage", {})
        tokens_used = usage.get("input_tokens", 0) + usage.get("output_tokens", 0)
        return ChatResponse(
            id=data.get("id", str(uuid.uuid4())),
            content=content,
            provider=AIProvider.ANTHROPIC,
            model=model,
            tokens_used=tokens_used,
            created_at=datetime.now(timezone.utc),
        )

    async def _anthropic_stream(
        self, request: ChatRequest, model: str
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        system_prompt = ""
        messages = []
        for m in request.messages:
            if m.role == MessageRole.SYSTEM:
                system_prompt = m.content
            else:
                messages.append({"role": m.role.value, "content": m.content})

        payload: dict = {
            "model": model,
            "max_tokens": request.max_tokens,
            "messages": messages,
            "stream": True,
        }
        if system_prompt:
            payload["system"] = system_prompt

        chunk_id = str(uuid.uuid4())
        async with httpx.AsyncClient(timeout=120) as client:
            async with client.stream(
                "POST",
                "https://api.anthropic.com/v1/messages",
                json=payload,
                headers={
                    "x-api-key": settings.anthropic_api_key,
                    "anthropic-version": "2023-06-01",
                    "Content-Type": "application/json",
                },
            ) as resp:
                resp.raise_for_status()
                async for line in resp.aiter_lines():
                    if not line.startswith("data:"):
                        continue
                    raw = line[len("data:"):].strip()
                    try:
                        import json

                        data = json.loads(raw)
                        event_type = data.get("type", "")
                        if event_type == "content_block_delta":
                            delta = data.get("delta", {}).get("text", "")
                            if delta:
                                yield ChatStreamChunk(
                                    id=chunk_id,
                                    delta=delta,
                                    is_final=False,
                                    provider=AIProvider.ANTHROPIC,
                                )
                        elif event_type == "message_stop":
                            yield ChatStreamChunk(
                                id=chunk_id,
                                delta="",
                                is_final=True,
                                provider=AIProvider.ANTHROPIC,
                            )
                            return
                    except Exception:
                        continue

    # ------------------------------------------------------------------
    # Gemini
    # ------------------------------------------------------------------

    async def _gemini_chat(self, request: ChatRequest, model: str) -> ChatResponse:
        parts = []
        for m in request.messages:
            if m.role != MessageRole.SYSTEM:
                parts.append({"role": m.role.value, "parts": [{"text": m.content}]})

        payload = {
            "contents": parts,
            "generationConfig": {
                "maxOutputTokens": request.max_tokens,
                "temperature": request.temperature,
            },
        }
        url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/"
            f"{model}:generateContent?key={settings.gemini_api_key}"
        )
        async with httpx.AsyncClient(timeout=60) as client:
            resp = await client.post(url, json=payload)
            resp.raise_for_status()
            data = resp.json()

        candidates = data.get("candidates", [])
        content = ""
        if candidates:
            content = candidates[0]["content"]["parts"][0].get("text", "")
        tokens_used = (
            data.get("usageMetadata", {}).get("totalTokenCount", 0)
        )
        return ChatResponse(
            id=str(uuid.uuid4()),
            content=content,
            provider=AIProvider.GEMINI,
            model=model,
            tokens_used=tokens_used,
            created_at=datetime.now(timezone.utc),
        )

    async def _gemini_stream(
        self, request: ChatRequest, model: str
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        """
        Gemini streaming via streamGenerateContent endpoint (SSE).
        Falls back to a single request + local chunking on error.
        """
        chunk_id = str(uuid.uuid4())
        parts = []
        for m in request.messages:
            if m.role != MessageRole.SYSTEM:
                parts.append({"role": m.role.value, "parts": [{"text": m.content}]})

        payload = {
            "contents": parts,
            "generationConfig": {
                "maxOutputTokens": request.max_tokens,
                "temperature": request.temperature,
            },
        }
        url = (
            f"https://generativelanguage.googleapis.com/v1beta/models/"
            f"{model}:streamGenerateContent?key={settings.gemini_api_key}&alt=sse"
        )
        async with httpx.AsyncClient(timeout=120) as client:
            async with client.stream("POST", url, json=payload) as resp:
                resp.raise_for_status()
                async for line in resp.aiter_lines():
                    if not line.startswith("data:"):
                        continue
                    raw = line[len("data:"):].strip()
                    try:
                        import json

                        data = json.loads(raw)
                        candidates = data.get("candidates", [])
                        if candidates:
                            text = candidates[0]["content"]["parts"][0].get("text", "")
                            if text:
                                yield ChatStreamChunk(
                                    id=chunk_id,
                                    delta=text,
                                    is_final=False,
                                    provider=AIProvider.GEMINI,
                                )
                    except Exception:
                        continue
        yield ChatStreamChunk(id=chunk_id, delta="", is_final=True, provider=AIProvider.GEMINI)

    # ------------------------------------------------------------------
    # Ollama (local)
    # ------------------------------------------------------------------

    def _ollama_base_url(self) -> str:
        return getattr(settings, "ollama_base_url", "http://localhost:11434")

    async def _ollama_chat(self, request: ChatRequest, model: str) -> ChatResponse:
        messages = [{"role": m.role.value, "content": m.content} for m in request.messages]
        payload = {
            "model": model,
            "messages": messages,
            "stream": False,
            "options": {"temperature": request.temperature, "num_predict": request.max_tokens},
        }
        async with httpx.AsyncClient(timeout=120) as client:
            resp = await client.post(f"{self._ollama_base_url()}/api/chat", json=payload)
            resp.raise_for_status()
            data = resp.json()

        content = data.get("message", {}).get("content", "")
        return ChatResponse(
            id=str(uuid.uuid4()),
            content=content,
            provider=AIProvider.OLLAMA,
            model=model,
            tokens_used=data.get("eval_count", 0),
            created_at=datetime.now(timezone.utc),
        )

    async def _ollama_stream(
        self, request: ChatRequest, model: str
    ) -> AsyncGenerator[ChatStreamChunk, None]:
        import json

        messages = [{"role": m.role.value, "content": m.content} for m in request.messages]
        payload = {
            "model": model,
            "messages": messages,
            "stream": True,
            "options": {"temperature": request.temperature, "num_predict": request.max_tokens},
        }
        chunk_id = str(uuid.uuid4())
        async with httpx.AsyncClient(timeout=120) as client:
            async with client.stream(
                "POST", f"{self._ollama_base_url()}/api/chat", json=payload
            ) as resp:
                resp.raise_for_status()
                async for line in resp.aiter_lines():
                    if not line:
                        continue
                    try:
                        data = json.loads(line)
                        delta = data.get("message", {}).get("content", "")
                        done = data.get("done", False)
                        if delta:
                            yield ChatStreamChunk(
                                id=chunk_id,
                                delta=delta,
                                is_final=False,
                                provider=AIProvider.OLLAMA,
                            )
                        if done:
                            yield ChatStreamChunk(
                                id=chunk_id,
                                delta="",
                                is_final=True,
                                provider=AIProvider.OLLAMA,
                            )
                            return
                    except Exception:
                        continue


# Singleton used by routers
ai_service = AIService()
