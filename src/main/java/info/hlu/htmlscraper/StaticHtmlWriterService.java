package info.hlu.htmlscraper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class StaticHtmlWriterService {

    public void writeDashboardHtml(List<ScrapedData> scrapedDataList, String filePath) {
        if (scrapedDataList == null) {
            log.warn("Scraped data list is null. Skipping static HTML generation.");
            return;
        }

        // Sort data by date descending, similar to original logic
        List<ScrapedData> sortedData = scrapedDataList.stream()
            .sorted(Comparator.comparing(ScrapedData::date).reversed())
            .toList();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <title>上海市闵行区人民政府征收土地方案公告</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/milligram/1.4.1/milligram.min.css\">\n");
        html.append("    <script src=\"https://unpkg.com/tablesort@5.2.1/dist/tablesort.min.js\"></script>\n");
        html.append("    <style>\n");
        html.append("        table, th, td { border: 1px solid #ccc; border-collapse: collapse; padding: 8px; }\n");
        html.append("        th { cursor: pointer; background-color: #f9f9f9; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <h1>上海市闵行区人民政府征收土地方案公告</h1>\n");
        html.append("    <table id=\"sortableTable\">\n");
        html.append("        <thead>\n");
        html.append("            <tr>\n");
        html.append("                <th>公告</th>\n");
        html.append("                <th>日期</th>\n");
        html.append("            </tr>\n");
        html.append("        </thead>\n");
        html.append("        <tbody>\n");

        for (ScrapedData item : sortedData) {
            html.append("            <tr>\n");
            html.append("                <td><a href=\"").append(item.url()).append("\" target=\"_blank\">").append(item.text()).append("</a></td>\n");
            html.append("                <td>").append(item.date()).append("</td>\n");
            html.append("            </tr>\n");
        }

        html.append("        </tbody>\n");
        html.append("    </table>\n");
        html.append("    <script>\n");
        html.append("        document.addEventListener(\"DOMContentLoaded\", function () {\n");
        html.append("            var table = new Tablesort(document.getElementById('sortableTable'));\n");
        html.append("            setTimeout(() => {\n");
        html.append("              const dateColumnTh = document.querySelectorAll('#sortableTable th')[1];\n");
        html.append("              if (dateColumnTh) {\n");
        html.append("                table.sort(dateColumnTh, 'desc');\n"); // Default sort by date desc
        html.append("              }\n");
        html.append("            }, 100);\n");
        html.append("        });\n");
        html.append("    </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(filePath).toFile()))) {
            writer.write(html.toString());
            log.info("Static dashboard written to: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to write static dashboard HTML to {}: {}", filePath, e.getMessage(), e);
        }
    }
}
