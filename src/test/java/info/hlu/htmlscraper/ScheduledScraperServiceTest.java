package info.hlu.htmlscraper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledScraperServiceTest {

    @Mock
    DynamicScraperService mockDynamicScraperService;

    @Mock
    StaticHtmlWriterService mockStaticHtmlWriterService;

    @InjectMocks
    ScheduledScraperService scheduledScraperService;

    @Test
    void testPerformScrapingTask() {
        // Prepare a sample List<ScrapedData>
        List<ScrapedData> sampleData = new ArrayList<>();
        sampleData.add(new ScrapedData("Test Item 1", "http://example.com/item1", "2023-01-01"));
        sampleData.add(new ScrapedData("Test Item 2", "http://example.com/item2", "2023-01-02"));

        // Define behavior for mockDynamicScraperService.scrape()
        when(mockDynamicScraperService.scrape()).thenReturn(sampleData);

        // Define behavior for mockStaticHtmlWriterService.writeDashboardHtml()
        // This is a void method, so we can use doNothing() or just verify its call.
        // Using doNothing() can be useful if there's a desire to ensure no exceptions are thrown from the mock.
        doNothing().when(mockStaticHtmlWriterService).writeDashboardHtml(anyList(), anyString());

        // Call the method to be tested
        scheduledScraperService.performScrapingTask();

        // Verify that mockDynamicScraperService.scrape() was called exactly once
        verify(mockDynamicScraperService, times(1)).scrape();

        // Verify that mockStaticHtmlWriterService.writeDashboardHtml() was called exactly once
        // with the sampleData and the expected file path "docs/dashboard.html"
        verify(mockStaticHtmlWriterService, times(1)).writeDashboardHtml(sampleData, "docs/dashboard.html");
    }
}
