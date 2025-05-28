package info.hlu.htmlscraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DynamicScraperService {

    private static final String BASE_URL = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/";
    private static final String KEYWORD = "闵行";
    @Getter
    private final Map<String, String> matchedLinks = new HashMap<>();
    @Getter
    private final Map<String, String> linksDate = new HashMap<>();
    @Value("${search.pages-size:-30}")
    private int searchPagesSize;

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

    public void scrape() {
        matchedLinks.clear();
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        String formattedDate = sdf.format(new Date());
        html.append("""
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta charset="UTF-8">
                                <title>上海市闵行区人民政府征收土地方案公告</title>
                                <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/milligram/1.4.1/milligram.min.css">
                                <script src="https://unpkg.com/tablesort@5.2.1/dist/tablesort.min.js"></script>
                                <style>
                                    table, th, td { border: 1px solid #ccc; border-collapse: collapse; padding: 8px; }
                                    th { cursor: pointer; background-color: #f9f9f9; }
                                </style>
                            </head>
                            <body>
                        """)
                .append("<h1>上海市闵行区人民政府征收土地方案公告 最近更新时间: ")
                .append(formattedDate)
                .append("</h1>")
                .append("""
                        <table id="sortableTable">
                                    <thead>
                                        <tr>
                                            <th>公告</th>
                                            <th>日期</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                        """);
        linksDate.entrySet().stream()
                .sorted((e1, e2) -> {
                    // desc by date
                    int valueCompare = e2.getValue().compareTo(e1.getValue());
                    // desc by key
                    return (valueCompare != 0) ? valueCompare : e2.getKey().compareTo(e1.getKey());
                })
                .forEach(entry -> {
                    String text = entry.getKey();
                    String date = entry.getValue();
                    String url = linksDate.get(text);
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
                                setTimeout(() => {
                                  table.sort(1, true);
                                }, 100);
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
}
