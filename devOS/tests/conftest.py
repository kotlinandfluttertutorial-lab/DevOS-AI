"""Pytest configuration and shared fixtures."""
import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture(scope="module")
def client() -> TestClient:
    """Synchronous test client for the FastAPI app."""
    with TestClient(app) as c:
        yield c
