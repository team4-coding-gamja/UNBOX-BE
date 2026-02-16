from typing import List
from fastapi import APIRouter, Depends, HTTPException
from src.api.dependencies import get_market_service
from src.service.market_service import MarketService
from src.dto.market_dto import MarketPriceResponse, MarketDetailResponse
from src.dto.common_dto import BaseResponse
from src.scheduler.jobs import refresh_target_shoes_price
from src.scheduler.runner import TARGET_SHOES_LIST

router = APIRouter(prefix="/market-price", tags=["Market Intelligence"])

@router.post("/refresh/{product_name}", response_model=BaseResponse[List[MarketPriceResponse]])
async def refresh_price(
    product_name: str,
    service: MarketService = Depends(get_market_service)
):
    """
    특정 상품의 시세를 실시간으로 크롤링하여 DB를 갱신합니다.
    (상위 N개 상품 + 옵션별 가격 포함)
    """
    try:
        result = await service.refresh_market_price(product_name)
        return BaseResponse(
            success=True,
            message=f"Successfully updated price for {product_name}",
            data=result
        )
    except Exception as e:
        # HTTPException은 FastAPI가 자동으로 잡아서 처리
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/{product_name}/latest", response_model=BaseResponse[MarketPriceResponse])
def get_latest(
    product_name: str,
    service: MarketService = Depends(get_market_service)
):
    """
    DB에 저장된 가장 최근 시세를 가져옵니다. (단일 건 - 대표값)
    """
    result = service.get_latest_price(product_name)
    if not result:
        return BaseResponse(success=False, message="No data found", data=None)
    
    return BaseResponse(success=True, message="Success", data=result)

@router.get("/{model_number}/detail", response_model=BaseResponse[MarketDetailResponse])
def get_detail(
    model_number: str,
    service: MarketService = Depends(get_market_service)
):
    """
    [New] 모델번호로 해당 상품의 상세 시세(사이즈별 최신 가격)를 조회합니다.
    Java 서비스 연동용 API
    """
    result = service.get_market_price_detail(model_number)
    
    if not result:
        return BaseResponse(success=False, message=f"No data found for model {model_number}", data=None)
        
    return BaseResponse(success=True, message="Success", data=result)

@router.post("/scheduler/test-run", response_model=BaseResponse)
async def trigger_scheduler_job_manually():
    """
    [테스트용] 스케줄러 Job을 수동으로 즉시 실행합니다.
    """
    await refresh_target_shoes_price(TARGET_SHOES_LIST)
    return BaseResponse(success=True, message="Manual Job Execution Completed", data=None)