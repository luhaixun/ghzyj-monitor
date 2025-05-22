package info.hlu.htmlscraper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    DynamicScraperService mockScraperService;

    @Mock
    Model mockModel;

    @InjectMocks
    DashboardController dashboardController;

    @Test
    void testShowDashboard() {
        // Prepare a sample List<ScrapedData>
        List<ScrapedData> sampleList = new ArrayList<>();
        sampleList.add(new ScrapedData("Test Text", "http://example.com", "2023-01-01"));

        // When mockScraperService.getLatestScrapedData() is called, return the sample list
        when(mockScraperService.getLatestScrapedData()).thenReturn(sampleList);

        // Call dashboardController.showDashboard(mockModel)
        String viewName = dashboardController.showDashboard(mockModel);

        // Verify that mockScraperService.getLatestScrapedData() was called
        verify(mockScraperService).getLatestScrapedData();

        // Verify that mockModel.addAttribute("scrapedItems", sampleList) was called
        verify(mockModel).addAttribute("scrapedItems", sampleList);

        // Assert that the method returns the string "dashboard"
        assertEquals("dashboard", viewName);
    }
}
