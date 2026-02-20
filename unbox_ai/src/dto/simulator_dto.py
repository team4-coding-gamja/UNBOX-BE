from datetime import date, datetime
from typing import Dict, Optional
from pydantic import BaseModel, Field


class SimulatorRunRequest(BaseModel):
    days: int = Field(7, ge=1, le=365)
    users: int = Field(2000, ge=1, le=200000)
    avg_sessions_per_user: float = Field(1.2, ge=0.1, le=20.0)
    seller_ratio: float = Field(0.3, ge=0.0, le=1.0)
    seed: Optional[int] = None
    experiment_id: Optional[str] = None
    variant_splits: Optional[Dict[str, float]] = None
    start_date: Optional[date] = None


class SimulatorRunResponse(BaseModel):
    users: int
    sessions: int
    events_inserted: int
    by_event: Dict[str, int]
    started_at: datetime
    ended_at: datetime
