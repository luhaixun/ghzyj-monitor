package info.hlu.htmlscraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DynamicScraperService {

    private static final String BASE_URL = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/";
    private static final int TOTAL_PAGES = 10;
    private static final String KEYWORD = "闵行";

    @Getter
    private final Map<String, String> matchedLinks = new TreeMap<>();
    @Getter
    private final Map<String, String> linksDate = new HashMap<>();

    public void scrape() {
        matchedLinks.clear();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .setViewportSize(1920, 1080)
            );
            Page page = context.newPage();

            for (int pageCount = 0; pageCount <= TOTAL_PAGES; pageCount++) {
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
                    if (text.contains(KEYWORD)) {
                        Locator link = h4.locator("a");
                        String href = link.getAttribute("href");
                        String absUrl = URI.create(BASE_URL).resolve(href).toString();
                        matchedLinks.put(text, absUrl);
                        linksDate.put(text, extractDateFromUrl(href));
                        foundLinks++;
                    }
                }
                log.debug("Found matching links {} out of {} from page {}", foundLinks, count, pageCount);
            }
            log.info("Scraping complete. Found {} matched links.", matchedLinks.size());

            browser.close();
            writeStaticHtml("docs/dashboard.html");
            System.exit(0);
        } catch (Exception e) {
            log.error("Scraping failed. error: {}", e.getMessage());
        }
    }

    private void writeStaticHtml(String output) {
        StringBuilder html = new StringBuilder();
        html.append("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <title>Matched Links</title>
                        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/milligram/1.4.1/milligram.min.css">
                        <script src="https://unpkg.com/tablesort@5.2.1/dist/tablesort.min.js"></script>
                        <style>
                            table, th, td { border: 1px solid #ccc; border-collapse: collapse; padding: 8px; }
                            th { cursor: pointer; background-color: #f9f9f9; }
                        </style>
                    </head>
                    <body>
                        <h1>Matched Links Dashboard</h1>
                        <table id="sortableTable">
                            <thead>
                                <tr>
                                    <th>公告</th>
                                    <th>日期</th>
                                </tr>
                            </thead>
                            <tbody>
                """);
        matchedLinks.forEach((text, url) -> {
            String date = linksDate.getOrDefault(text, "Unknown");
            html.append("<tr>")
                    .append("<td><a href=\"").append(url).append("\" target=\"_blank\">").append(text).append("</a></td>")
                    .append("<td>").append(date).append("</td>")
                    .append("</tr>");
        });

        html.append("""
                    </tbody>
                        </table>
                        <script>
                            document.addEventListener("DOMContentLoaded", function () {
                                var table = new Tablesort(document.getElementById('sortableTable'));
                                table.sort(1, true); // descending by date
                            });
                        </script>
                    </body>
                    </html>
                """);

        try {
            String outputPath = Paths.get(output).toString(); // for GitHub Pages
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
                writer.write(html.toString());
            }
            log.info("Static dashboard written to: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to write static dashboard HTML: {}", e.getMessage(), e);
        }
    }

    private static String extractDateFromUrl(String url) {
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
