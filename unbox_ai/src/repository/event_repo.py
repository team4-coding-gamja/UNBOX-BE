from typing import Iterable
from sqlalchemy.orm import Session
from src.models.analytics_event import AnalyticsEvent


class AnalyticsEventRepository:
    def __init__(self, db: Session):
        self.db = db

    def bulk_insert(self, events: Iterable[AnalyticsEvent]) -> int:
        items = list(events)
        if not items:
            return 0
        self.db.bulk_save_objects(items)
        self.db.commit()
        return len(items)
