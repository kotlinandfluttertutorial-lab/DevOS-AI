"""
RAG (Retrieval-Augmented Generation) router.

POST /v1/rag/query              — query with repository code context
GET  /v1/rag/status/{repo_id}  — RAG index status for a repository
"""
from fastapi import APIRouter

from app.models.ai_chat import RAGQuery, RAGResult
from app.services.ai_service import ai_service

router = APIRouter()

# In-memory index status store.
# In production this would be populated by the indexing WorkManager job.
_index_status: dict[str, dict] = {}


@router.post("/rag/query", response_model=RAGResult, tags=["rag"])
async def rag_query(query: RAGQuery) -> RAGResult:
    """
    Query a repository's code context using RAG.
    Returns an AI-generated answer grounded in the repository's source files.
    Full vector search will be enabled after DA-129 (vector index build).
    """
    return await ai_service.rag_query(query)


@router.get("/rag/status/{repo_id}", tags=["rag"])
async def rag_index_status(repo_id: str) -> dict:
    """
    Return the RAG index status for the given repository.
    Possible statuses: not_indexed | indexing | ready | error
    """
    status = _index_status.get(
        repo_id,
        {
            "repository_id": repo_id,
            "status": "not_indexed",
            "indexed_files": 0,
            "total_files": 0,
            "message": "Repository has not been indexed yet. Trigger indexing via the repository sync endpoint.",
        },
    )
    return status
