"""
KREAM 시세 크롤링 스크립트 (독립 프로세스)
- Selenium + BeautifulSoup 사용
"""
import sys
import re
import json
import argparse
import time
from urllib.parse import quote
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from bs4 import BeautifulSoup

def setup_driver(debug: bool = False):
    """크롬 드라이버 설정"""
    header_user = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36"
    options_ = Options()
    options_.add_argument(f"User-Agent={header_user}")
    options_.add_experimental_option('excludeSwitches', ["enable-logging"])
    options_.add_argument("--window-size=1920,1080")

    if not debug:
        options_.add_argument("--headless=new")
    else:
        options_.add_experimental_option("detach", True)

    return webdriver.Chrome(options=options_)

def get_product_urls(driver, keyword: str, limit: int = 10) -> list:
    """검색 결과에서 상위 N개의 상품 URL 반환"""
    url = f"https://kream.co.kr/search?keyword={quote(keyword)}"
    print(f"[Info] Searching: {url}", file=sys.stderr)
    driver.get(url)
    time.sleep(2)
    
    urls = []
    try:
        # 스크롤 조금 내려서 로딩 유도
        driver.find_element(By.TAG_NAME, 'body').send_keys(Keys.PAGE_DOWN)
        time.sleep(1)
        
        # 상품 링크 찾기
        elements = driver.find_elements(By.XPATH, "//a[contains(@href, '/products/')]")
        seen = set()
        for el in elements:
            href = el.get_attribute("href")
            if href and "/products/" in href and "select" not in href:
                if href not in seen:
                    # 상세 페이지 URL 정규화
                    if not href.startswith("http"):
                         # selenium get_attribute('href')는 보통 절대경로를 주지만, 혹시 모를 대비
                         pass 
                    
                    seen.add(href)
                    urls.append(href)
                    if len(urls) >= limit:
                        break
    except Exception as e:
        print(f"[Warning] Failed to find products: {e}", file=sys.stderr)
        
    return urls

def extract_details(driver, url: str) -> dict:
    """상세 페이지 + 스톡 페이지 크롤링"""
    data = {
        "url": url, 
        "model_number": None, 
        "name": None, 
        "options": []
    }
    
    # 1. 상세 페이지 (모델번호, 상품명)
    print(f"[Info] Navigating to Detail URL: {url}", file=sys.stderr)
    try:
        driver.get(url)
        time.sleep(1.5)
        
        soup = BeautifulSoup(driver.page_source, "html.parser")
        
        # 상품명 (title 태그 활용이 제일 안전)
        # 예: Nike Dunk Low Retro Black - KREAM
        title = soup.title.string if soup.title else ""
        if " - " in title:
            data["name"] = title.split(" - ")[0].strip()
            
        # 모델번호 추출
        for tag in soup.find_all(string=re.compile("모델번호")):
             if tag and "모델번호" in tag:
                clean = tag.replace("모델번호", "").strip()
                if clean:
                    data["model_number"] = clean
                    break
    except Exception as e:
        print(f"[Warning] Detail extraction error: {e}", file=sys.stderr)

    # 2. 스톡 페이지 (옵션별 가격)
    try:
        match = re.search(r'/products/(\d+)', url)
        if match:
            pid = match.group(1)
            stock_url = f"https://kream.co.kr/products/select/{pid}/stock?productId={pid}"
            print(f"[Info] Navigating to Stock URL: {stock_url}", file=sys.stderr)
            
            driver.get(stock_url)
            time.sleep(2)
            
            soup = BeautifulSoup(driver.page_source, "html.parser")
            grid_items = soup.select(".layout-grid-horizontal-equal > div")
            
            if not grid_items:
                driver.find_element(By.TAG_NAME, 'body').send_keys(Keys.PAGE_DOWN)
                time.sleep(1)
                soup = BeautifulSoup(driver.page_source, "html.parser")
                grid_items = soup.select(".layout-grid-horizontal-equal > div")

            options = []
            for item in grid_items:
                texts = [p.text.strip() for p in item.select("p")]
                size = None
                price = None
                
                for t in texts:
                    if not t: continue
                    if "원" in t:
                        clean_price = re.sub(r'[^\d]', '', t)
                        if clean_price: price = int(clean_price)
                    elif any(x in t for x in ["구매", "판매", "입찰", "%", "전체", "체결"]):
                        continue
                    elif re.match(r'^\d+(\.\d+)?$', t) or re.match(r'^[XSMLXL\d]+$', t, re.IGNORECASE):
                         size = t
                    elif "모든 사이즈" in t:
                        size = "All"
                
                if price and not size and texts:
                     potential_size = texts[0]
                     if "원" not in potential_size and len(potential_size) < 15:
                         size = potential_size

                if size and price:
                    options.append({"size": size, "price": price})
            
            data["options"] = options
            
    except Exception as e:
        print(f"[Warning] Stock extraction error: {e}", file=sys.stderr)
        
    return data

def main():
    sys.stdout.reconfigure(encoding='utf-8')
    sys.stderr.reconfigure(encoding='utf-8')

    parser = argparse.ArgumentParser()
    parser.add_argument("product_name")
    parser.add_argument("--detail", action="store_true")
    parser.add_argument("--debug", action="store_true")
    args = parser.parse_args()

    driver = setup_driver(args.debug)
    
    try:
        if args.detail:
            # 다중 상품 크롤링 (Limit=5)
            urls = get_product_urls(driver, args.product_name, limit=10)
            print(f"[Info] Found {len(urls)} products for '{args.product_name}'", file=sys.stderr)
            
            results = []
            for u in urls:
                d = extract_details(driver, u)
                results.append(d)
                
            print(json.dumps(results, ensure_ascii=False))
            print(f"[Info] Collected info for {len(results)} products", file=sys.stderr)
            
        else:
            # 기존 단일 가격 모드 (호환성 유지)
            urls = get_product_urls(driver, args.product_name, limit=1)
            if urls:
                d = extract_details(driver, urls[0])
                # 최저가 계산
                min_price = 0
                if d["options"]:
                    prices = [o["price"] for o in d["options"]]
                    min_price = min(prices)
                print(f"{min_price}")
            else:
                print("0")
                
    except Exception as e:
        if args.detail:
            print("[]")
        else:
            print("0")
        print(f"[Error] {e}", file=sys.stderr)
    finally:
        driver.quit()

if __name__ == "__main__":
    main()
