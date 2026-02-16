import asyncio
import logging
import subprocess
import sys
import os
import json

logger = logging.getLogger(__name__)

class MarketCrawler:
    """
    외부 커머스 사이트의 시세를 긁어오는 서비스
    * Windows 환경 이벤트 루프 충돌(NotImplementedError)을 완벽하게 해결하기 위해
      Playwright/Selenium 로직을 별도 프로세스(src/scripts/single_crawler.py)로 분리하여 실행합니다.
    """
    
    async def fetch_item_price(self, product_name: str) -> int:
        """
        [기존] 검색 결과 첫 번째 상품의 대표 가격만 수집
        """
        return await asyncio.to_thread(self._run_crawler_script, product_name, detail_mode=False)

    async def fetch_item_detail_prices(self, product_name: str) -> list[dict]:
        """
        [확장] 상세 페이지 크롤링: 검색된 상위 N개 상품의 상세 정보 수집
        Returns: [{"model_number": "...", "options": [...]}, ...]
        """
        return await asyncio.to_thread(self._run_crawler_script, product_name, detail_mode=True)

    def _run_crawler_script(self, product_name: str, detail_mode: bool = False):
        try:
            # 현재 프로젝트 루트 경로 (cwd)
            cwd = os.getcwd()
            
            # 실행 명령어: python -m src.scripts.single_crawler "상품명" [--detail]
            cmd = [sys.executable, "-m", "src.scripts.single_crawler", product_name]
            
            if detail_mode:
                cmd.append("--detail")
            
            logger.info(f"[Crawler] Executing detail={detail_mode} for: {product_name}")
            
            # subprocess 실행 (타임아웃 설정 가능)
            # 윈도우 한글 출력을 위해 encoding='utf-8' 명시
            result = subprocess.run(
                cmd, 
                cwd=cwd,
                capture_output=True, 
                text=True, 
                encoding='utf-8'
            )
            
            # 스크립트 stderr 로그 출력 (디버깅용)
            if result.stderr:
                # 너무 긴 로그는 잘라서 출력하거나 필요한 정보만 필터링
                logs = result.stderr.strip()
                if logs and ("[Info]" in logs or "[Error]" in logs):
                    logger.info(f"[Crawler Script] {logs}")
            
            if result.returncode != 0:
                logger.error(f"[Crawler] Script exited with code {result.returncode}")
                return [] if detail_mode else 0
            
            output = result.stdout.strip()
            if not output:
                logger.warning(f"[Crawler] No output from script for {product_name}")
                return [] if detail_mode else 0
                
            # 결과 파싱
            if detail_mode:
                try:
                    data = json.loads(output)
                    # 리스트 형태임
                    product_count = len(data) if isinstance(data, list) else 0
                    logger.info(f"[Crawler] Success: {product_name} -> {product_count} products collected")
                    return data
                except json.JSONDecodeError:
                    logger.error(f"[Crawler] Invalid JSON output: {output[:100]}...")
                    return []
            else:
                try:
                    price = int(output)
                    logger.info(f"[Crawler] Success: {product_name} -> {price}")
                    return price
                except ValueError:
                    logger.error(f"[Crawler] Invalid int output: {output}")
                    return 0
                
        except Exception as e:
            logger.error(f"[Crawler] Process Execution Error: {e}")
            return [] if detail_mode else 0

# 싱글톤 인스턴스
market_crawler = MarketCrawler()