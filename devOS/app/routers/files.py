"""File tree and content endpoints — DA-127.

Used by the Android Code Intelligence feature to render the file explorer and
display file content.

Route map
---------
GET /v1/repositories/{repo_id}/tree  — full recursive file tree
GET /v1/repositories/{repo_id}/file  — single file content (?path=src/Main.kt)
"""
from __future__ import annotations

from typing import Literal, Optional

from fastapi import APIRouter, HTTPException, Query, status
from pydantic import BaseModel

# Reference the shared in-memory store from the repositories router so both
# routers stay consistent (no duplicate state).
from app.routers.repositories import _repositories

router = APIRouter()


# ---------------------------------------------------------------------------
# Schemas
# ---------------------------------------------------------------------------


class FileNode(BaseModel):
    """A single node in the repository file tree."""

    name: str
    path: str
    type: Literal["file", "directory"]
    size: Optional[int] = None
    children: Optional[list["FileNode"]] = None

    model_config = {"populate_by_name": True}


# Rebuild model after forward-reference
FileNode.model_rebuild()

# ---------------------------------------------------------------------------
# Stub tree — 3-level sample structure
# ---------------------------------------------------------------------------

_STUB_TREE = FileNode(
    name="root",
    path="/",
    type="directory",
    children=[
        FileNode(name="README.md", path="README.md", type="file", size=1024),
        FileNode(name="build.gradle.kts", path="build.gradle.kts", type="file", size=768),
        FileNode(
            name="src",
            path="src",
            type="directory",
            children=[
                FileNode(
                    name="main",
                    path="src/main",
                    type="directory",
                    children=[
                        FileNode(
                            name="kotlin",
                            path="src/main/kotlin",
                            type="directory",
                            children=[
                                FileNode(
                                    name="com.devos",
                                    path="src/main/kotlin/com/devos",
                                    type="directory",
                                    children=[
                                        FileNode(
                                            name="Main.kt",
                                            path="src/main/kotlin/com/devos/Main.kt",
                                            type="file",
                                            size=512,
                                        ),
                                        FileNode(
                                            name="ui",
                                            path="src/main/kotlin/com/devos/ui",
                                            type="directory",
                                            children=[
                                                FileNode(
                                                    name="HomeScreen.kt",
                                                    path="src/main/kotlin/com/devos/ui/HomeScreen.kt",
                                                    type="file",
                                                    size=2048,
                                                ),
                                            ],
                                        ),
                                    ],
                                )
                            ],
                        )
                    ],
                ),
                FileNode(
                    name="test",
                    path="src/test",
                    type="directory",
                    children=[
                        FileNode(
                            name="kotlin",
                            path="src/test/kotlin",
                            type="directory",
                            children=[
                                FileNode(
                                    name="MainTest.kt",
                                    path="src/test/kotlin/MainTest.kt",
                                    type="file",
                                    size=256,
                                )
                            ],
                        )
                    ],
                ),
            ],
        ),
    ],
)

_STUB_FILE_CONTENT = """\
// Stub file content — real content served from indexed repository.
package com.devos

fun main() {
    println("DevOS AI — placeholder")
}
"""

# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------


@router.get(
    "/repositories/{repo_id}/tree",
    response_model=FileNode,
    tags=["files"],
    summary="Get recursive file tree",
)
async def get_file_tree(repo_id: str) -> FileNode:
    """Return the full recursive file tree for a repository.

    For now returns a 3-level stub tree; real implementation will build the
    tree from the indexed file store in a later ticket.
    """
    if repo_id not in _repositories:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Repository not found",
        )
    return _STUB_TREE


@router.get(
    "/repositories/{repo_id}/file",
    tags=["files"],
    summary="Get single file content",
)
async def get_file(
    repo_id: str,
    path: str = Query(..., description="File path relative to repository root, e.g. src/main/kotlin/Main.kt"),
) -> dict:
    """Return the raw content of a single file.

    For now returns stub content; real implementation will read from the
    indexed repository in a later ticket.
    """
    if repo_id not in _repositories:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Repository not found",
        )
    return {
        "path": path,
        "content": _STUB_FILE_CONTENT,
        "encoding": "utf-8",
        "size": len(_STUB_FILE_CONTENT),
    }
