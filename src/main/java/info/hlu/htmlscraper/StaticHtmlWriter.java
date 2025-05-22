package info.hlu.htmlscraper;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
public class StaticHtmlWriter {

    public void writeStaticHtml(List<ScrapedData> scrapedDataList, String output) {
        StringBuilder html = new StringBuilder();
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
                        <h1>上海市闵行区人民政府征收土地方案公告</h1>
                        <table id="sortableTable">
                            <thead>
                                <tr>
                                    <th>公告</th>
                                    <th>日期</th>
                                </tr>
                            </thead>
                            <tbody>
                """);

        scrapedDataList.stream()
                .sorted((d1, d2) -> d2.date().compareTo(d1.date())) // Sort by date descending
                .forEach(data -> {
                    html.append("<tr>")
                            .append("<td><a href=\"").append(data.url()).append("\" target=\"_blank\">").append(data.text()).append("</a></td>")
                            .append("<td>").append(data.date()).append("</td>")
                            .append("</tr>");
                });

        html.append("""
                    </tbody>
                        </table>
                        <script>
                            document.addEventListener("DOMContentLoaded", function () {
                                var table = new Tablesort(document.getElementById('sortableTable'));
                                setTimeout(() => {
                                  // Sort by the second column (date) in descending order initially
                                  table.sort(document.querySelector('#sortableTable th:nth-child(2)'), 'desc');
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
