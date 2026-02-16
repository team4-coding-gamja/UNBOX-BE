from fastapi import Request
from fastapi.responses import JSONResponse
from src.dto.common_dto import BaseResponse

class CustomException(Exception):
    """기본 커스텀 예외 클래스"""
    def __init__(self, message: str, status_code: int = 400):
        self.message = message
        self.status_code = status_code

class NotFoundException(CustomException):
    def __init__(self, message: str = "Not Found"):
        super().__init__(message, status_code=404)

# Global Exception Handler (main.py에서 사용)
async def custom_exception_handler(request: Request, exc: CustomException):
    return JSONResponse(
        status_code=exc.status_code,
        content=BaseResponse(
            success=False,
            message=exc.message,
            data=None
        ).model_dump()
    )