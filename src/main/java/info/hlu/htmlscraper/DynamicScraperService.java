package info.hlu.htmlscraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet; // Added import
import java.util.List;
import java.util.Set; // Added import
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
public class DynamicScraperService {

    private static final String BASE_URL = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/";

    @Value("${search.pages-size:-30}")
    private int searchPagesSize;

    private static final String KEYWORD = "闵行";
    private volatile List<ScrapedData> cachedScrapedData = new ArrayList<>();
    private volatile Set<String> processedItemIdentifiers = new HashSet<>(); // Added field

    public List<ScrapedData> scrape() {
        List<ScrapedData> scrapedDataList = new ArrayList<>();
        boolean stopEarly = false; // Flag to signal stopping
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
            );
            Page page = context.newPage();

            for (int pageCount = 0; pageCount <= searchPagesSize; pageCount++) {
                String url = pageCount == 0 ? BASE_URL + "index.html" : BASE_URL + "index_" + pageCount + ".html";
                long start = System.currentTimeMillis();
                page.navigate(url);
                page.waitForLoadState(LoadState.NETWORKIDLE);
                log.debug("Paring page {} cost {} ms", pageCount, System.currentTimeMillis() - start);
                Locator h4s = page.locator("xpath=/html/body/div[2]/div[3]/div/div/div/div/div[2]/h4");

                int count = h4s.count();
                int foundLinks = 0;
                for (int j = 0; j < count; j++) {
                    Locator h4 = h4s.nth(j);
                    String text = h4.textContent();
                    // fetch all or filter by keyword
                    if (!StringUtils.hasText(KEYWORD) || text.contains(KEYWORD)) {
                        Locator link = h4.locator("a");
                        String href = link.getAttribute("href");
                        String absUrl = URI.create(BASE_URL).resolve(href).toString();
                        String date = extractDateFromUrl(href);

                        // NOTE: Early stopping logic assumes that if an item from a previous scrape
                        // is encountered (based on its URL), no new, uncaptured items will appear 
                        // after this item on the current page, or on any subsequent pages.
                        // If this website behavior changes, this optimization might lead to missed data.
                        // Check if this item's URL was already processed in the previous scrape
                        if (this.processedItemIdentifiers != null && !this.processedItemIdentifiers.isEmpty() && this.processedItemIdentifiers.contains(absUrl)) {
                            log.info("Item with URL '{}' was already processed. Signaling to stop further scraping for this cycle.", absUrl);
                            stopEarly = true;
                            break; // Break inner loop (j)
                        }
                        
                        scrapedDataList.add(new ScrapedData(text, absUrl, date));
                        foundLinks++;
                    }
                } // End inner loop

                if (stopEarly) {
                    log.info("Stopping page iteration early due to finding an already processed item.");
                    break; // Break outer loop (pageCount)
                }
                log.debug("Found matching links {} out of {} from page {}", foundLinks, count, pageCount);
            } // End outer loop
            log.info("Scraping complete. Found {} matched links.", scrapedDataList.size());

            browser.close();
        } catch (Exception e) {
            log.error("Scraping failed. error: {}", e.getMessage());
        }

        // New logic: Before updating the cache with new data,
        // populate processedItemIdentifiers from the *current* cachedScrapedData (which is from the n-1 scrape)
        Set<String> previousScrapeIdentifiers = new HashSet<>();
        if (this.cachedScrapedData != null && !this.cachedScrapedData.isEmpty()) {
            for (ScrapedData oldItem : this.cachedScrapedData) {
                if (oldItem.url() != null) { // Ensure URL is not null
                    previousScrapeIdentifiers.add(oldItem.url());
                }
            }
        }
        this.processedItemIdentifiers = previousScrapeIdentifiers;
        log.info("Updated processedItemIdentifiers with {} URLs from the previous scrape.", this.processedItemIdentifiers.size());

        // This existing line then updates cachedScrapedData with the results of the current (nth) scrape
        this.cachedScrapedData = new ArrayList<>(scrapedDataList); 
        return scrapedDataList;
    }

    public List<ScrapedData> getLatestScrapedData() {
        return new ArrayList<>(this.cachedScrapedData); // Return a copy
    }

    // Changed from private static to package-private static for testing
    static String extractDateFromUrl(String url) {
        // Regex to match tYYYYMMDD_ pattern
        Pattern pattern = Pattern.compile("t(\\d{4})(\\d{2})(\\d{2})_");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String year = matcher.group(1);
            String month = matcher.group(2);
            String day = matcher.group(3);
            return year + "年" + month + "月" + day + "日";
        }

        return "Unknown Date";
    }
}
