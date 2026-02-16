from typing import List, Optional
from src.repository.market_repo import MarketRepository
from src.service.crawler_service import market_crawler
from src.dto.market_dto import MarketPriceResponse, MarketDetailResponse

class MarketService:
    def __init__(self, market_repo: MarketRepository):
        self.market_repo = market_repo

    async def refresh_market_price(self, product_name: str) -> List[MarketPriceResponse]:
        """
        [고도화됨] 특정 상품 검색 결과(상위 N개)의 상세 시세 정보(옵션별 가격)를 갱신합니다.
        Returns: 저장된 MarketPriceResponse 리스트
        """
        products_data = await market_crawler.fetch_item_detail_prices(product_name)
        
        if not products_data:
            return []

        results = []
        
        for product_info in products_data:
            crawled_name = product_info.get("name")
            target_name = crawled_name if crawled_name else product_name
            
            model_number = product_info.get("model_number")
            options = product_info.get("options", [])
            
            for item in options:
                size = item.get("size")
                price = item.get("price")
                
                saved_entity = self.market_repo.save_detailed_info(
                    product_name=target_name,
                    size=size,
                    price=price,
                    model_number=model_number
                )
                results.append(MarketPriceResponse.model_validate(saved_entity))

        return results

    def get_latest_price(self, product_name: str) -> MarketPriceResponse | None:
        """
        특정 상품의 가장 최근 시세 정보를 조회합니다. (단일 건)
        """
        market_data = self.market_repo.find_by_product_name(product_name)
        if not market_data:
            return None
        return MarketPriceResponse.model_validate(market_data)

    def get_market_price_detail(self, model_number: str) -> Optional[MarketDetailResponse]:
        """
        [New] 모델번호로 상품 상세 시세 정보(옵션별 최신가)를 조회합니다.
        """
        result = self.market_repo.find_detail_by_model(model_number)
        if not result:
            return None
        return MarketDetailResponse(**result)