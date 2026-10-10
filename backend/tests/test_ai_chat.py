"""
Tests for AI chat, RAG, and agent endpoints.

All tests exercise stub/no-key mode so they run without real API credentials.
"""
import json

import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

_CHAT_PAYLOAD = {
    "messages": [{"role": "user", "content": "Hello, what is Python?"}],
    "provider": "openai",
}

_STREAM_PAYLOAD = {
    "messages": [{"role": "user", "content": "Stream test"}],
    "provider": "openai",
    "stream": True,
}

_RAG_PAYLOAD = {
    "query": "How does authentication work?",
    "repository_id": "repo-abc-123",
    "top_k": 3,
}

_AGENT_PAYLOAD = {
    "prompt": "Find all TODO comments in the codebase",
    "tools": [
        {
            "name": "search_code",
            "description": "Search for text patterns in source files",
            "parameters": {"type": "object", "properties": {"pattern": {"type": "string"}}},
        }
    ],
    "provider": "openai",
}


# ---------------------------------------------------------------------------
# POST /v1/chat — stub mode (no API key configured)
# ---------------------------------------------------------------------------


def test_chat_stub_mode():
    """When no API key is set the service returns a stub content string."""
    response = client.post("/v1/chat", json=_CHAT_PAYLOAD)
    assert response.status_code == 200, response.text
    data = response.json()

    # Response must contain the required fields
    assert "id" in data
    assert "content" in data
    assert "provider" in data
    assert "model" in data
    assert "tokens_used" in data
    assert "created_at" in data

    # Stub mode returns tokens_used == 0 and a non-empty content
    assert data["tokens_used"] == 0
    assert len(data["content"]) > 0
    # Stub content must hint that the key is missing
    assert "not configured" in data["content"].lower() or "stub" in data["content"].lower()


def test_chat_provider_field_echoed():
    """The response provider must match what was sent."""
    response = client.post("/v1/chat", json=_CHAT_PAYLOAD)
    assert response.status_code == 200
    assert response.json()["provider"] == "openai"


def test_chat_anthropic_stub():
    payload = {**_CHAT_PAYLOAD, "provider": "anthropic"}
    response = client.post("/v1/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["provider"] == "anthropic"
    assert data["tokens_used"] == 0


def test_chat_gemini_stub():
    payload = {**_CHAT_PAYLOAD, "provider": "gemini"}
    response = client.post("/v1/chat", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["provider"] == "gemini"


# ---------------------------------------------------------------------------
# POST /v1/chat/stream — stub SSE mode
# ---------------------------------------------------------------------------


def test_chat_stream_stub_mode():
    """SSE stream should return multiple data: lines ending with [DONE]."""
    response = client.post(
        "/v1/chat/stream",
        json=_STREAM_PAYLOAD,
    )
    assert response.status_code == 200
    assert "text/event-stream" in response.headers.get("content-type", "")

    body = response.text
    lines = [l for l in body.splitlines() if l.startswith("data:")]

    # Must have at least one data chunk plus the [DONE] sentinel
    assert len(lines) >= 2
    assert lines[-1].strip() == "data: [DONE]"


def test_chat_stream_chunks_are_valid_json():
    """Each data: line (except [DONE]) must be valid ChatStreamChunk JSON."""
    response = client.post("/v1/chat/stream", json=_STREAM_PAYLOAD)
    assert response.status_code == 200

    body = response.text
    data_lines = [l[len("data:"):].strip() for l in body.splitlines() if l.startswith("data:")]

    for raw in data_lines:
        if raw == "[DONE]":
            continue
        chunk = json.loads(raw)
        assert "id" in chunk
        assert "delta" in chunk
        assert "is_final" in chunk
        assert "provider" in chunk


def test_chat_stream_ends_with_final_chunk():
    """The last real chunk (before [DONE]) should have is_final=True."""
    response = client.post("/v1/chat/stream", json=_STREAM_PAYLOAD)
    body = response.text
    data_lines = [l[len("data:"):].strip() for l in body.splitlines() if l.startswith("data:")]
    # Filter out [DONE] and parse
    chunks = [json.loads(raw) for raw in data_lines if raw != "[DONE]"]
    assert len(chunks) > 0
    assert chunks[-1]["is_final"] is True


# ---------------------------------------------------------------------------
# POST /v1/rag/query — stub mode
# ---------------------------------------------------------------------------


def test_rag_query_stub():
    """RAG query should return a stub answer and at least one source hint."""
    response = client.post("/v1/rag/query", json=_RAG_PAYLOAD)
    assert response.status_code == 200, response.text
    data = response.json()

    assert "query" in data
    assert "answer" in data
    assert "sources" in data
    assert "provider" in data
    assert "tokens_used" in data

    assert data["query"] == _RAG_PAYLOAD["query"]
    assert len(data["answer"]) > 0
    assert isinstance(data["sources"], list)


def test_rag_query_includes_repo_id_in_answer():
    """The stub answer should reference the repository_id so it's traceable."""
    response = client.post("/v1/rag/query", json=_RAG_PAYLOAD)
    data = response.json()
    assert _RAG_PAYLOAD["repository_id"] in data["answer"]


def test_rag_status_unindexed_repo():
    """A repository that has not been indexed returns not_indexed status."""
    response = client.get("/v1/rag/status/unknown-repo-xyz")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "not_indexed"
    assert data["repository_id"] == "unknown-repo-xyz"


# ---------------------------------------------------------------------------
# GET /v1/providers — security check
# ---------------------------------------------------------------------------


def test_list_providers_returns_all():
    """All four providers should be listed."""
    response = client.get("/v1/providers")
    assert response.status_code == 200
    data = response.json()
    names = {p["name"] for p in data}
    assert names == {"openai", "anthropic", "gemini", "ollama"}


def test_list_providers_never_returns_key_values():
    """
    SECURITY: The response must only contain has_key (boolean),
    never the actual key string or any prefix of it.
    """
    response = client.get("/v1/providers")
    assert response.status_code == 200
    raw = response.text  # check the raw JSON body

    # These strings must never appear in the response
    dangerous_patterns = ["sk-", "AIza", "Bearer ", "api_key", "apiKey"]
    for pattern in dangerous_patterns:
        assert pattern not in raw, f"Potential key leak: '{pattern}' found in /providers response"

    # Each provider entry must have has_key as a boolean, not a string key
    for provider in response.json():
        assert isinstance(provider["has_key"], bool)
        assert "key" not in provider or provider.get("key") is None


def test_list_providers_has_required_fields():
    """Each provider entry must have name, has_key, default_model, available."""
    response = client.get("/v1/providers")
    for provider in response.json():
        assert "name" in provider
        assert "has_key" in provider
        assert "default_model" in provider
        assert "available" in provider


# ---------------------------------------------------------------------------
# POST /v1/agents/execute — stub mode
# ---------------------------------------------------------------------------


def test_agent_execute_stub():
    """Agent execution should return a completed status with steps."""
    response = client.post("/v1/agents/execute", json=_AGENT_PAYLOAD)
    assert response.status_code == 200, response.text
    data = response.json()

    assert "id" in data
    assert data["status"] == "completed"
    assert isinstance(data["steps"], list)
    assert len(data["steps"]) > 0
    assert "final_answer" in data
    assert data["final_answer"] is not None


def test_agent_execute_returns_step_structure():
    """Each step must have step_number and thought fields."""
    response = client.post("/v1/agents/execute", json=_AGENT_PAYLOAD)
    steps = response.json()["steps"]
    for step in steps:
        assert "step_number" in step
        assert "thought" in step


def test_agent_status_after_execute():
    """Agent status endpoint should return the stored result."""
    # Execute first
    exec_resp = client.post("/v1/agents/execute", json=_AGENT_PAYLOAD)
    assert exec_resp.status_code == 200
    agent_id = exec_resp.json()["id"]

    # Fetch status
    status_resp = client.get(f"/v1/agents/{agent_id}/status")
    assert status_resp.status_code == 200
    assert status_resp.json()["id"] == agent_id


def test_agent_status_unknown_id():
    """Unknown agent_id must return 404."""
    response = client.get("/v1/agents/non-existent-agent-id/status")
    assert response.status_code == 404
