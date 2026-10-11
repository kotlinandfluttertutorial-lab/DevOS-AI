"""Repository API endpoint tests — DA-127.

Tests use the synchronous TestClient (no asyncio required) because all
endpoints are currently backed by in-memory dicts.

Each test that creates data uses a unique full_name to stay isolated even
when the module-level store is shared.
"""
import uuid

import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.routers import repositories as repo_router

client = TestClient(app)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _unique_name() -> str:
    """Return a unique owner/repo string so tests do not collide."""
    return f"test-owner/repo-{uuid.uuid4().hex[:8]}"


def _create_repo(full_name: str | None = None) -> dict:
    """POST /v1/repositories and return the response body."""
    fn = full_name or _unique_name()
    payload = {
        "name": fn.split("/")[-1],
        "full_name": fn,
        "clone_url": f"https://github.com/{fn}.git",
        "source": "github",
        "description": "Test repository",
        "default_branch": "main",
        "is_private": False,
    }
    resp = client.post("/v1/repositories", json=payload)
    assert resp.status_code == 201, resp.text
    return resp.json()


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(autouse=True)
def clear_store():
    """Reset the in-memory store before each test."""
    repo_router._repositories.clear()
    repo_router._index_status.clear()
    yield
    repo_router._repositories.clear()
    repo_router._index_status.clear()


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------


def test_list_repositories_empty():
    """GET /v1/repositories returns empty list when no repos exist."""
    response = client.get("/v1/repositories")
    assert response.status_code == 200
    data = response.json()
    assert data["items"] == []
    assert data["total"] == 0
    assert data["page"] == 1


def test_create_repository():
    """POST /v1/repositories creates a new repository with correct fields."""
    fn = _unique_name()
    payload = {
        "name": "my-repo",
        "full_name": fn,
        "clone_url": f"https://github.com/{fn}.git",
        "source": "github",
        "description": "A test repo",
        "default_branch": "develop",
        "is_private": True,
    }
    response = client.post("/v1/repositories", json=payload)
    assert response.status_code == 201
    data = response.json()
    assert data["id"]
    assert data["full_name"] == fn
    assert data["source"] == "github"
    assert data["default_branch"] == "develop"
    assert data["is_private"] is True
    assert data["index_status"] == "pending"
    assert data["file_count"] == 0


def test_create_repository_duplicate_full_name():
    """POST /v1/repositories with a duplicate full_name returns 400."""
    fn = _unique_name()
    _create_repo(fn)
    # Second attempt with same full_name must fail
    payload = {
        "name": "other",
        "full_name": fn,
        "clone_url": "https://github.com/other.git",
        "source": "github",
    }
    response = client.post("/v1/repositories", json=payload)
    assert response.status_code == 400


def test_get_repository():
    """GET /v1/repositories/{id} returns the correct repository."""
    created = _create_repo()
    repo_id = created["id"]

    response = client.get(f"/v1/repositories/{repo_id}")
    assert response.status_code == 200
    data = response.json()
    assert data["id"] == repo_id
    assert data["full_name"] == created["full_name"]


def test_get_repository_not_found():
    """GET /v1/repositories/{id} returns 404 for unknown id."""
    response = client.get(f"/v1/repositories/{uuid.uuid4()}")
    assert response.status_code == 404


def test_update_repository():
    """PUT /v1/repositories/{id} updates description and default_branch."""
    created = _create_repo()
    repo_id = created["id"]

    update_payload = {"description": "Updated description", "default_branch": "release"}
    response = client.put(f"/v1/repositories/{repo_id}", json=update_payload)
    assert response.status_code == 200
    data = response.json()
    assert data["description"] == "Updated description"
    assert data["default_branch"] == "release"
    # Other fields unchanged
    assert data["full_name"] == created["full_name"]


def test_update_repository_not_found():
    """PUT /v1/repositories/{id} returns 404 for unknown id."""
    response = client.put(f"/v1/repositories/{uuid.uuid4()}", json={"description": "X"})
    assert response.status_code == 404


def test_delete_repository():
    """DELETE /v1/repositories/{id} returns 204 and removes the record."""
    created = _create_repo()
    repo_id = created["id"]

    delete_resp = client.delete(f"/v1/repositories/{repo_id}")
    assert delete_resp.status_code == 204

    # Subsequent GET must return 404
    get_resp = client.get(f"/v1/repositories/{repo_id}")
    assert get_resp.status_code == 404


def test_delete_repository_not_found():
    """DELETE /v1/repositories/{id} returns 404 for unknown id."""
    response = client.delete(f"/v1/repositories/{uuid.uuid4()}")
    assert response.status_code == 404


def test_trigger_index():
    """POST /v1/repositories/{id}/index returns 202 with status_url."""
    created = _create_repo()
    repo_id = created["id"]

    response = client.post(f"/v1/repositories/{repo_id}/index", json={"force_reindex": False})
    assert response.status_code == 202
    data = response.json()
    assert "status_url" in data
    assert repo_id in data["status_url"]


def test_trigger_index_not_found():
    """POST /v1/repositories/{id}/index returns 404 for unknown id."""
    response = client.post(f"/v1/repositories/{uuid.uuid4()}/index", json={})
    assert response.status_code == 404


def test_get_index_status_before_trigger():
    """GET .../index/status returns PENDING before any indexing is triggered."""
    created = _create_repo()
    repo_id = created["id"]

    response = client.get(f"/v1/repositories/{repo_id}/index/status")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "pending"
    assert data["repository_id"] == repo_id


def test_get_index_status_after_trigger():
    """GET .../index/status returns INDEXING after trigger."""
    created = _create_repo()
    repo_id = created["id"]

    client.post(f"/v1/repositories/{repo_id}/index", json={})

    response = client.get(f"/v1/repositories/{repo_id}/index/status")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "indexing"
    assert data["started_at"] is not None


def test_list_repositories_pagination():
    """GET /v1/repositories respects page and page_size parameters."""
    # Create 5 repos
    for _ in range(5):
        _create_repo()

    resp_page1 = client.get("/v1/repositories?page=1&page_size=3")
    assert resp_page1.status_code == 200
    p1 = resp_page1.json()
    assert len(p1["items"]) == 3
    assert p1["total"] == 5
    assert p1["has_next"] is True

    resp_page2 = client.get("/v1/repositories?page=2&page_size=3")
    p2 = resp_page2.json()
    assert len(p2["items"]) == 2
    assert p2["has_next"] is False


def test_list_repositories_filter_by_source():
    """GET /v1/repositories?source=gitlab returns only gitlab repos."""
    # Create one github, one gitlab
    fn_gh = _unique_name()
    fn_gl = _unique_name()
    client.post(
        "/v1/repositories",
        json={
            "name": "gh-repo",
            "full_name": fn_gh,
            "clone_url": f"https://github.com/{fn_gh}.git",
            "source": "github",
        },
    )
    client.post(
        "/v1/repositories",
        json={
            "name": "gl-repo",
            "full_name": fn_gl,
            "clone_url": f"https://gitlab.com/{fn_gl}.git",
            "source": "gitlab",
        },
    )

    response = client.get("/v1/repositories?source=gitlab")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 1
    assert data["items"][0]["source"] == "gitlab"


def test_list_files():
    """GET /v1/repositories/{id}/files returns stub file list."""
    created = _create_repo()
    repo_id = created["id"]

    response = client.get(f"/v1/repositories/{repo_id}/files")
    assert response.status_code == 200
    data = response.json()
    assert data["total"] > 0
    assert "items" in data


def test_get_file_content():
    """GET /v1/repositories/{id}/files/content returns file content."""
    created = _create_repo()
    repo_id = created["id"]

    response = client.get(f"/v1/repositories/{repo_id}/files/content?path=src/Main.kt")
    assert response.status_code == 200
    data = response.json()
    assert data["path"] == "src/Main.kt"
    assert "content" in data
    assert data["encoding"] == "utf-8"
