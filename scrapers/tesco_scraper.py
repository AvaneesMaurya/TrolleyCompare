import time
from playwright.sync_api import sync_playwright
from bs4 import BeautifulSoup

def scrape_tesco_milk():
    """
    Navigates to Tesco Ireland for 'milk', waits for the page to fully render, 
    and extracts product names and prices.
    """
    search_query = "milk"
    url = f"https://www.tesco.ie/groceries/en-IE/search?query={search_query}"
    
    with sync_playwright() as p:
        # Launch headless browser with anti-detection configurations
        browser = p.chromium.launch(
            headless=True,
            args=["--disable-blink-features=AutomationControlled"]
        )
        
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            viewport={"width": 1920, "height": 1080}
        )
        
        page = context.new_page()
        
        try:
            print(f"[Tesco Scraper] Navigating to: {url}")
            page.goto(url, timeout=60000, wait_until="domcontentloaded")
            
            # Explicit wait for dynamic content/hydration to load completely
            print("[Tesco Scraper] Waiting 7 seconds for page elements to render...")
            time.sleep(7)
            
            # Scroll down slightly to trigger lazy-loaded product tiles if any
            page.evaluate("window.scrollBy(0, 600)")
            time.sleep(2)
            
            # Parse the fully rendered HTML content
            content = page.content()
            soup = BeautifulSoup(content, 'html.parser')
            
            products = []
            
            # Target product cards using robust selectors
            tiles = soup.select('li.product-tile, div.product-card, [data-auto="product-tile"], li[data-qa="product-tile"]')
            
            if not tiles:
                # Fallback generic container search for items on the page
                tiles = soup.find_all('li', class_=lambda c: c and ('item' in str(c).lower() or 'product' in str(c).lower()))

            print(f"[Tesco Scraper] Scan complete. Found {len(tiles)} product containers. Filtering for milk...")
            
            for tile in tiles[:15]: # Process top matching results
                text_block = tile.get_text()
                if "milk" in text_block.lower():
                    # Extract product title
                    title_elem = tile.find(['h3', 'a', 'span'], class_=lambda c: c and ('title' in c.lower() or 'heading' in c.lower()))
                    title = title_elem.get_text(strip=True) if title_elem else "N/A"
                    
                    # Extract regular price value
                    price_elem = tile.find('span', class_=lambda c: c and ('value' in c.lower() or 'price' in c.lower()))
                    price = price_elem.get_text(strip=True) if price_elem else "N/A"
                    
                    # Check for Clubcard promotional price
                    club_elem = tile.find('span', class_=lambda c: c and 'clubcard' in c.lower())
                    club_price = club_elem.get_text(strip=True) if club_elem else None

                    if title != "N/A" and price != "N/A":
                        products.append({
                            "store": "Tesco",
                            "title": title,
                            "regular_price": price,
                            "club_price": club_price
                        })
            
            return products if products else {"error": "Page loaded and waited, but no milk product cards matched selectors."}
            
        except Exception as e:
            return {"error": str(e)}
            
        finally:
            browser.close()

if __name__ == "__main__":
    result = scrape_tesco_milk()
    print("Scraper Output:", result)