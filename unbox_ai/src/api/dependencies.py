from fastapi import Depends
from sqlalchemy.orm import Session
from src.core.database import get_db
from src.repository.event_repo import AnalyticsEventRepository
from src.service.simulator_service import SimulatorService


def get_simulator_service(db: Session = Depends(get_db)) -> SimulatorService:
    repository = AnalyticsEventRepository(db)
    return SimulatorService(repository)
