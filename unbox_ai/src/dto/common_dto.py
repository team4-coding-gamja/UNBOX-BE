from pydantic import BaseModel
from typing import TypeVar, Generic, Optional
from datetime import datetime

T = TypeVar("T")

class BaseResponse(BaseModel, Generic[T]):
    """Java의 CustomApiResponse와 동일한 역할"""
    success: bool
    message: str
    data: Optional[T] = None
    server_time: datetime = datetime.utcnow()