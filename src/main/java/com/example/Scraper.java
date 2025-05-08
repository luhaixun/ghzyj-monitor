package com.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.ArrayList;
import java.util.List;

public class Scraper {
    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();  // Automatically handle the correct ChromeDriver version

        // Set up Chrome options (no need for remote-allow-origins argument)
        ChromeOptions options = new ChromeOptions();
        // Uncomment the following line if you want to run it headless (without opening a browser window)
        options.addArguments("--headless");

        WebDriver driver = new ChromeDriver(options);

        List<String> results = new ArrayList<>();

        try {
            // Loop through pages 1 to 10
            for (int i = 0; i < 10; i++) {
                String url = (i == 0)
                        ? "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/index.html"
                        : "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/index_" + i + ".html";

                System.out.println("Checking page: " + url);
                driver.get(url);

                // Wait for dynamic content to load
                Thread.sleep(3000);

                // Find all links on the page
                List<WebElement> links = driver.findElements(By.tagName("a"));
                for (WebElement link : links) {
                    String text = link.getText();
                    if (text.contains("闵行区")) {
                        String href = link.getAttribute("href");
                        results.add("Match found: " + text + " → " + href);
                    }
                }
            }

            // Generate HTML page with the results
            String htmlContent = generateHTML(results);

            // Save results to HTML file
            try (java.io.FileWriter writer = new java.io.FileWriter("dashboard/index.html")) {
                writer.write(htmlContent);
                System.out.println("Results saved to dashboard/index.html");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    // Generate an HTML content with the search results
    private static String generateHTML(List<String> results) {
        StringBuilder html = new StringBuilder();

        // Basic HTML structure
        html.append("<html>")
                .append("<head><title>Scraping Results</title></head>")
                .append("<body>")
                .append("<h1>Results for 闵行区</h1>")
                .append("<ul>");

        for (String result : results) {
            html.append("<li>").append(result).append("</li>");
        }

        html.append("</ul>")
                .append("</body>")
                .append("</html>");

        return html.toString();
    }
}
