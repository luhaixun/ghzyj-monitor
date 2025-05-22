package info.hlu.htmlscraper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StaticHtmlWriterServiceTest {

    StaticHtmlWriterService staticHtmlWriterService = new StaticHtmlWriterService();

    @TempDir
    Path tempDir; // JUnit 5 temporary directory

    @Test
    void testWriteDashboardHtml_WithData() throws IOException {
        Path outputFile = tempDir.resolve("dashboard.html");
        List<ScrapedData> data = Arrays.asList(
            new ScrapedData("Item 1", "http://example.com/1", "2023年01月01日"),
            new ScrapedData("Item 2", "http://example.com/2", "2023年01月03日"), // Later date
            new ScrapedData("Item 3", "http://example.com/3", "2023年01月02日")
        );

        staticHtmlWriterService.writeDashboardHtml(data, outputFile.toString());

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);

        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("<title>上海市闵行区人民政府征收土地方案公告</title>"));
        assertTrue(content.contains("<table id=\"sortableTable\">"));
        assertTrue(content.contains("Item 2")); // Should appear first due to sorting
        assertTrue(content.contains("http://example.com/2"));
        assertTrue(content.contains("2023年01月03日"));
        assertTrue(content.contains("Item 3"));
        assertTrue(content.contains("http://example.com/3"));
        assertTrue(content.contains("2023年01月02日"));
        assertTrue(content.contains("Item 1"));
        assertTrue(content.contains("http://example.com/1"));
        assertTrue(content.contains("2023年01月01日"));
        
        // Check order based on date
        int indexOfItem2 = content.indexOf("Item 2");
        int indexOfItem3 = content.indexOf("Item 3");
        int indexOfItem1 = content.indexOf("Item 1");

        // Ensure all items are found before checking order
        assertTrue(indexOfItem1 != -1, "Item 1 not found in HTML");
        assertTrue(indexOfItem2 != -1, "Item 2 not found in HTML");
        assertTrue(indexOfItem3 != -1, "Item 3 not found in HTML");

        assertTrue(indexOfItem2 < indexOfItem3, "Item 2 (Jan 03) should appear before Item 3 (Jan 02)");
        assertTrue(indexOfItem3 < indexOfItem1, "Item 3 (Jan 02) should appear before Item 1 (Jan 01)");
    }

    @Test
    void testWriteDashboardHtml_EmptyData() throws IOException {
        Path outputFile = tempDir.resolve("dashboard_empty.html");
        staticHtmlWriterService.writeDashboardHtml(Collections.emptyList(), outputFile.toString());

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("<title>上海市闵行区人民政府征收土地方案公告</title>"));
        assertTrue(content.contains("<table id=\"sortableTable\">"));
        // Check that the tbody is empty or contains no <tr><td> elements
        // A simple check is that it doesn't contain "<td><a href" which indicates a data row
        assertFalse(content.contains("<td><a href"), "Table body should be empty, no data rows expected.");
    }

    @Test
    void testWriteDashboardHtml_NullData() throws IOException {
        Path outputFile = tempDir.resolve("dashboard_null.html");
        
        // As per current StaticHtmlWriterService implementation, it logs a warning and returns early.
        // Thus, the file should not be created/modified if it was passed as a path.
        staticHtmlWriterService.writeDashboardHtml(null, outputFile.toString());

        assertFalse(Files.exists(outputFile), "File should not be created or modified for null data as the service should return early.");
    }
}
