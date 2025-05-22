package info.hlu.htmlscraper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicScraperServiceTest {

    // Using lenient stubs for mocks that might not be called in every test
    // Using deep stubs for Playwright objects to simplify chained calls
    @Mock(strictness = Mock.Strictness.LENIENT, answer = Answers.RETURNS_DEEP_STUBS)
    Playwright mockPlaywright;
    @Mock(strictness = Mock.Strictness.LENIENT, answer = Answers.RETURNS_DEEP_STUBS)
    Browser mockBrowser;
    @Mock(strictness = Mock.Strictness.LENIENT, answer = Answers.RETURNS_DEEP_STUBS)
    BrowserContext mockContext;
    @Mock(strictness = Mock.Strictness.LENIENT, answer = Answers.RETURNS_DEEP_STUBS)
    Page mockPage;
    @Mock(strictness = Mock.Strictness.LENIENT)
    Locator mockH4sLocator; // For h4 elements
    @Mock(strictness = Mock.Strictness.LENIENT)
    Locator mockH4Locator;   // For a single h4
    @Mock(strictness = Mock.Strictness.LENIENT)
    Locator mockALocator;    // For the <a> tag

    @InjectMocks
    DynamicScraperService dynamicScraperService;

    private MockedStatic<Playwright> playwrightStaticMock;

    @BeforeEach
    void setUp() {
        // Mock the static Playwright.create() method
        playwrightStaticMock = Mockito.mockStatic(Playwright.class);
        playwrightStaticMock.when(Playwright::create).thenReturn(mockPlaywright);

        // Mock the Playwright creation chain (using deep stubs simplifies this)
        when(mockPlaywright.firefox().launch(any(BrowserType.LaunchOptions.class))).thenReturn(mockBrowser);
        when(mockBrowser.newContext(any(Browser.NewContextOptions.class))).thenReturn(mockContext);
        when(mockContext.newPage()).thenReturn(mockPage);

        // Common page setup
        // Using doNothing() for void methods is good practice
        doNothing().when(mockPage).navigate(anyString());
        doNothing().when(mockPage).waitForLoadState(any(LoadState.class)); // Ensure correct any() matcher
        // Assuming the xpath locator is for h4s
        when(mockPage.locator(argThat(selector -> selector.toString().contains("xpath=")))).thenReturn(mockH4sLocator);


        // Set searchPagesSize using ReflectionTestUtils as it's @Value injected
        ReflectionTestUtils.setField(dynamicScraperService, "searchPagesSize", 0); // Test with 1 page (index 0)
    }

    @AfterEach
    void tearDown() {
        // Close the static mock
        if (playwrightStaticMock != null) {
            playwrightStaticMock.close();
        }
    }

    @Test
    void testScrapeSuccessful() {
        // Mock locators for a single item
        when(mockH4sLocator.count()).thenReturn(1);
        when(mockH4sLocator.nth(0)).thenReturn(mockH4Locator);
        when(mockH4Locator.textContent()).thenReturn("Test Entry 闵行");
        when(mockH4Locator.locator("a")).thenReturn(mockALocator);
        when(mockALocator.getAttribute("href")).thenReturn("t20230101_test.html");

        List<ScrapedData> result = dynamicScraperService.scrape();

        assertNotNull(result);
        assertEquals(1, result.size());
        ScrapedData item = result.get(0);
        assertEquals("Test Entry 闵行", item.text());
        assertTrue(item.url().endsWith("t20230101_test.html"), "URL should end with the href value");
        assertEquals("2023年01月01日", item.date());

        // Verify cache update
        List<ScrapedData> cachedData = dynamicScraperService.getLatestScrapedData();
        assertNotNull(cachedData, "Cached data should not be null");
        assertEquals(1, cachedData.size());
        assertEquals("Test Entry 闵行", cachedData.get(0).text());

        // Verify Playwright resources are closed
        verify(mockBrowser).close(); // Verifies browser.close() was called
        // playwright.close() is called implicitly by try-with-resources,
        // playwrightStaticMock.verify(Playwright::close) is not standard for static mocks.
        // We can verify our mockPlaywright's close was called if we had playwright.close();
        // For try-with-resources, verifying browser.close() implies the block was exited.
    }
    
    @Test
    void testScrapeWithNoMatchingKeyword() {
        when(mockH4sLocator.count()).thenReturn(1);
        when(mockH4sLocator.nth(0)).thenReturn(mockH4Locator);
        when(mockH4Locator.textContent()).thenReturn("Test Entry Without Keyword"); // No "闵行"
        when(mockH4Locator.locator("a")).thenReturn(mockALocator);
        when(mockALocator.getAttribute("href")).thenReturn("t20230102_other.html");

        List<ScrapedData> result = dynamicScraperService.scrape();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result list should be empty when keyword doesn't match");

        List<ScrapedData> cachedData = dynamicScraperService.getLatestScrapedData();
        assertTrue(cachedData.isEmpty(), "Cache should also be empty");
        verify(mockBrowser).close();
    }

    @Test
    void testScrapeWithMultipleEntriesAndPages() {
        ReflectionTestUtils.setField(dynamicScraperService, "searchPagesSize", 1); // Test with 2 pages (0 and 1)

        // Page 0 setup
        Locator mockH4sLocatorPage0 = mock(Locator.class);
        Locator mockH4LocatorPage0Item0 = mock(Locator.class);
        Locator mockALocatorPage0Item0 = mock(Locator.class);

        when(mockPage.locator(argThat(selector -> selector.toString().contains("xpath=")))).thenReturn(mockH4sLocatorPage0);
        when(mockH4sLocatorPage0.count()).thenReturn(1);
        when(mockH4sLocatorPage0.nth(0)).thenReturn(mockH4LocatorPage0Item0);
        when(mockH4LocatorPage0Item0.textContent()).thenReturn("Page 0 Item 闵行");
        when(mockH4LocatorPage0Item0.locator("a")).thenReturn(mockALocatorPage0Item0);
        when(mockALocatorPage0Item0.getAttribute("href")).thenReturn("t20230301_page0.html");
        
        // Simulate navigating to the next page, need to make mockPage.locator return different results
        // For simplicity, this example will assume mockPage.locator is called again for the second page
        // and we can re-stub it. In a more complex scenario, you might need to provide different mock Page objects
        // or use consecutive call stubbing if the same mockPage object is reused.

        // This part is tricky because the same mockPage is reused.
        // A better way would be to have newPage() return different mockPage instances for each loop,
        // or use more specific argument matchers for navigate/locator if URLs change.
        // For this test, we'll assume the first call to scrape() sets up for page 0,
        // and then we'll "re-mock" for page 1 before the second loop iteration happens.
        // This is a simplification.

        // For the first call to page.locator (page 0)
        when(mockPage.locator(argThat(selector -> selector.toString().contains("xpath=")))).thenReturn(mockH4sLocatorPage0);
        
        // Mocking for page 1 - this requires careful thought on how the loop interacts with mocks.
        // The current setup reuses the same mockPage. We'd need to make its behavior change
        // on subsequent calls if the locator string is identical.
        // Let's assume for this test, page.locator is called once per page.
        // This is a known simplification due to the static nature of the loop in scrape().
        Locator mockH4sLocatorPage1 = mock(Locator.class, "h4sPage1");
        Locator mockH4LocatorPage1Item0 = mock(Locator.class, "h4Page1Item0");
        Locator mockALocatorPage1Item0 = mock(Locator.class, "aPage1Item0");

        // We need to make mockPage.locator return different locators for different pages.
        // This can be done by matching on the navigation URL or by sequential stubbing.
        // Given the current structure, a simple sequential stubbing on locator might be:
        when(mockPage.locator(argThat(selector -> selector.toString().contains("xpath="))))
            .thenReturn(mockH4sLocatorPage0) // For first call (page 0)
            .thenReturn(mockH4sLocatorPage1); // For second call (page 1)

        when(mockH4sLocatorPage1.count()).thenReturn(1);
        when(mockH4sLocatorPage1.nth(0)).thenReturn(mockH4LocatorPage1Item0);
        when(mockH4LocatorPage1Item0.textContent()).thenReturn("Page 1 Item 闵行");
        when(mockH4LocatorPage1Item0.locator("a")).thenReturn(mockALocatorPage1Item0);
        when(mockALocatorPage1Item0.getAttribute("href")).thenReturn("t20230302_page1.html");

        List<ScrapedData> result = dynamicScraperService.scrape();

        assertNotNull(result);
        assertEquals(2, result.size(), "Should find items from both pages");
        assertEquals("Page 0 Item 闵行", result.get(0).text());
        assertEquals("Page 1 Item 闵行", result.get(1).text());
        
        verify(mockBrowser).close();
    }


    @Test
    void testGetLatestScrapedDataReturnsCopy() {
        // Setup initial cache state directly using ReflectionTestUtils
        ScrapedData originalItem = new ScrapedData("text", "url", "date");
        List<ScrapedData> initialCache = new ArrayList<>();
        initialCache.add(originalItem);
        ReflectionTestUtils.setField(dynamicScraperService, "cachedScrapedData", initialCache);

        List<ScrapedData> list1 = dynamicScraperService.getLatestScrapedData();
        assertNotNull(list1);
        assertEquals(1, list1.size());
        assertEquals("text", list1.get(0).text());

        // Try to modify the returned list
        try {
            list1.add(new ScrapedData("new", "new", "new"));
        } catch (UnsupportedOperationException e) {
            // This would happen if an unmodifiable list was returned, which is also a valid copy strategy.
            // The current implementation of getLatestScrapedData (new ArrayList<>(...)) returns a modifiable list.
        }
        
        // If list1 was modifiable, check that the cache itself wasn't modified
        if (list1.size() == 2) { // If add was successful
            List<ScrapedData> list2 = dynamicScraperService.getLatestScrapedData();
            assertEquals(1, list2.size(), "Cache should not be modified by external changes to the list copy.");
        } else { // If add failed (e.g. unmodifiable list)
             assertEquals(1, list1.size(), "List1 should remain unchanged if add operation failed.");
        }
    }

    @Test
    void testExtractDateFromUrl() {
        // Method is now package-private static
        assertEquals("2023年01月01日", DynamicScraperService.extractDateFromUrl("t20230101_test.html"));
        assertEquals("2024年12月31日", DynamicScraperService.extractDateFromUrl("abc_t20241231_xyz.aspx"));
        assertEquals("Unknown Date", DynamicScraperService.extractDateFromUrl("no_date_here.html"));
        assertEquals("Unknown Date", DynamicScraperService.extractDateFromUrl("t202301_short.html")); // Invalid date pattern
        assertEquals("Unknown Date", DynamicScraperService.extractDateFromUrl("t2023010_invalid.html")); // Invalid day
        assertEquals("Unknown Date", DynamicScraperService.extractDateFromUrl("t2023101_invalid.html")); // Invalid month
        assertEquals("Unknown Date", DynamicScraperService.extractDateFromUrl("")); // Empty string
        assertEquals("Unknown Date", DynamicScraperService.extractDateFromUrl(null)); // Null input
    }
    
    @Test
    void testScrapeHandlesPlaywrightException() {
        // Make Playwright.create() throw an exception
        playwrightStaticMock.when(Playwright::create).thenThrow(new RuntimeException("Playwright init failed"));

        List<ScrapedData> result = dynamicScraperService.scrape();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Result should be empty when Playwright fails to initialize");

        List<ScrapedData> cachedData = dynamicScraperService.getLatestScrapedData();
        assertTrue(cachedData.isEmpty(), "Cache should be empty when Playwright fails");
        
        // verify(mockBrowser, never()).close(); // Browser shouldn't be opened if Playwright.create() fails
        // The static mock for Playwright.create() means mockPlaywright itself is never "closed" by our test
    }
}
