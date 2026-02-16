from sqlalchemy.orm import Session
from sqlalchemy import text
from src.models.market_data import MarketData
from src.models.product_target import ProductTarget, ProductOptionTarget
from uuid import UUID

class MarketRepository:
    def __init__(self, db: Session):
        self.db = db

    def save(self, product_name: str, price: int):
        """[Deprecated] 이제 save_detailed_info를 사용하세요"""
        market_data = MarketData(product_name=product_name, price=price)
        self.db.add(market_data)
        self.db.commit()
        self.db.refresh(market_data)
        return market_data

    def save_detailed_info(self, product_name: str, size: str, price: int, model_number: str = None):
        """
        상품명, 사이즈, 가격, 모델번호(선택)를 받아 Product/Option/MarketData 엔티티 전체를 저장/갱신합니다.
        """
        target_model_number = model_number if model_number else product_name
        
        product = self.db.query(ProductTarget).filter(ProductTarget.model_number == target_model_number).first()
        
        if not product:
            product = ProductTarget(
                name=product_name,
                model_number=target_model_number
            )
            self.db.add(product)
            self.db.flush() 
        else:
            if product.name != product_name:
                product.name = product_name

        option = self.db.query(ProductOptionTarget).filter(
            ProductOptionTarget.product_id == product.id,
            ProductOptionTarget.size == size
        ).first()

        if not option:
            option = ProductOptionTarget(product_id=product.id, size=size)
            self.db.add(option)
            self.db.flush()

        market_data = MarketData(
            product_name=product_name,
            price=price,
            platform="KREAM",
            target_id=product.id,
            option_id=option.id
        )
        self.db.add(market_data)
        self.db.commit()
        self.db.refresh(market_data) # [Added Refresh]
        return market_data


    def find_all_active(self):
        return self.db.query(MarketData).filter(MarketData.is_deleted == False).all()

    def find_by_id(self, market_data_id: UUID):
        return self.db.query(MarketData).filter(
            MarketData.id == market_data_id,
            MarketData.is_deleted == False
        ).first()

    def find_by_product_name(self, name: str):
        return self.db.query(MarketData).filter(
            MarketData.product_name == name,
            MarketData.is_deleted == False
        ).order_by(MarketData.created_at.desc()).first()

    def find_detail_by_model(self, model_number: str) -> dict:
        """
        [New] 모델번호로 상품 상세 정보(옵션별 최신 가) 조회
        """
        # 1. Product 찾기
        product = self.db.query(ProductTarget).filter(ProductTarget.model_number == model_number).first()
        if not product:
            return None
            
        # 2. 각 옵션별 최신 가격 조회 (PostgreSQL Specific: DISTINCT ON)
        # m.option_id 별로 m.created_at DESC 정렬 후 첫 번째 row 선택
        sql = text("""
            SELECT DISTINCT ON (m.option_id)
                o.size,
                m.price,
                m.created_at
            FROM market_data m
            JOIN ai_product_options o ON m.option_id = o.id
            WHERE m.target_id = :target_id
            ORDER BY m.option_id, m.created_at DESC
        """)
        
        result = self.db.execute(sql, {"target_id": product.id}).fetchall()
        
        return {
            "model_number": product.model_number,
            "name": product.name,
            "options": [{"size": row[0], "price": row[1], "updated_at": row[2]} for row in result]
        }