from datetime import datetime, timezone
import uuid
from sqlalchemy import Column, DateTime, String
from sqlalchemy.dialects.postgresql import UUID, JSONB
from src.core.database import Base


class AnalyticsEvent(Base):
    __tablename__ = "events"
    __table_args__ = {"schema": "analytics"}

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, index=True)
    event_name = Column(String, nullable=False, index=True)
    user_id = Column(UUID(as_uuid=True), nullable=True, index=True)
    session_id = Column(String, nullable=True, index=True)
    source_service = Column(String, nullable=False)
    occurred_at = Column(DateTime(timezone=True), nullable=False, default=lambda: datetime.now(timezone.utc))
    properties = Column(JSONB, nullable=False, default=dict)
    experiment_id = Column(String, nullable=True, index=True)
    variant_id = Column(String, nullable=True, index=True)
