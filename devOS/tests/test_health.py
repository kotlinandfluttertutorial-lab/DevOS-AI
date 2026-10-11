"""Health endpoint tests."""
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    response = client.get("/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "version" in data
    assert "environment" in data


def test_health_version_format():
    response = client.get("/v1/health")
    data = response.json()
    # Version should follow semver-like pattern
    parts = data["version"].split(".")
    assert len(parts) == 3
