package info.hlu.htmlscraper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScrapedDataTest {
    @Test
    void testRecordInstantiationAndAccessors() {
        String text = "Test Text";
        String url = "http://example.com";
        String date = "2023-01-01";
        ScrapedData data = new ScrapedData(text, url, date);

        assertEquals(text, data.text());
        assertEquals(url, data.url());
        assertEquals(date, data.date());
    }
}
