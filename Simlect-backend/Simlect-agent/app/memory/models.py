from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

def empty_summary() -> dict[str, Any]:
    return {
        "version": 1,
        "narrative": "",
        "facts": {
            "goal": None,
            "budget": None,
            "preferences": [],
            "constraints": [],
            "decisions": [],
            "openQuestions": [],
        },
    }

def empty_state() -> dict[str, Any]:
    return {
        "consultProduct": None,
        "pendingAction": None,
        "lastToolResults": {
            "searchedProducts": [],
            "searchedProductNames": [],
            "queriedOrders": [],
            "viewedProductIds": [],
        },
        "lastSearchResults": [],
        "turnCount": 0,
        "summaryLastMessageId": 0,
        "estimatedTokens": 0,
    }

@dataclass
class SessionMemory:
    user_id: str
    summary: dict[str, Any] = field(default_factory=empty_summary)
    state: dict[str, Any] = field(default_factory=empty_state)

    @classmethod
    def from_storage(cls, user_id: str, summary: dict | None, state: dict | None) -> SessionMemory:
        mem = cls(user_id=user_id)
        if summary:
            mem.summary = summary
        if state:
            mem.state = state
        return mem

    @property
    def summary_last_message_id(self) -> int:
        return int(self.state.get("summaryLastMessageId") or 0)

    @summary_last_message_id.setter
    def summary_last_message_id(self, value: int) -> None:
        self.state["summaryLastMessageId"] = int(value)
