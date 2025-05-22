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
import java.util.HashSet; // Added import
import java.util.List;
import java.util.Set; // Added import

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
        // 1. Set an initial state for cachedScrapedData (n-1 scrape results)
        List<ScrapedData> initialCachedItems = new ArrayList<>();
        String oldCachedUrl = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/old_cached.html";
        initialCachedItems.add(new ScrapedData("Old Cached Text", oldCachedUrl, "old_date"));
        ReflectionTestUtils.setField(dynamicScraperService, "cachedScrapedData", new ArrayList<>(initialCachedItems));
        // Ensure processedItemIdentifiers is empty before this specific scrape call updates it
        ReflectionTestUtils.setField(dynamicScraperService, "processedItemIdentifiers", new HashSet<>());

        // 2. Mock Playwright to return a new item for the current scrape
        String newScrapedItemHref = "t20230101_test.html";
        String newScrapedItemAbsUrl = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/" + newScrapedItemHref;
        when(mockH4sLocator.count()).thenReturn(1);
        when(mockH4sLocator.nth(0)).thenReturn(mockH4Locator);
        when(mockH4Locator.textContent()).thenReturn("Test Entry 闵行");
        when(mockH4Locator.locator("a")).thenReturn(mockALocator);
        when(mockALocator.getAttribute("href")).thenReturn(newScrapedItemHref);

        // 3. Call scrape
        List<ScrapedData> result = dynamicScraperService.scrape();

        // 4. Assertions on the returned list (current scrape's results)
        assertNotNull(result);
        assertEquals(1, result.size());
        ScrapedData item = result.get(0);
        assertEquals("Test Entry 闵行", item.text());
        assertEquals(newScrapedItemAbsUrl, item.url());
        assertEquals("2023年01月01日", item.date());

        // 5. Assertions on the new state of cachedScrapedData (should be same as `result`)
        List<ScrapedData> currentCache = dynamicScraperService.getLatestScrapedData();
        assertNotNull(currentCache);
        assertEquals(1, currentCache.size());
        assertEquals(newScrapedItemAbsUrl, currentCache.get(0).url());

        // 6. Assertions on the state of processedItemIdentifiers *after* the scrape
        // It should now contain URLs from the initialCachedItems (n-1 scrape)
        @SuppressWarnings("unchecked")
        Set<String> processedIdsAfterScrape = (Set<String>) ReflectionTestUtils.getField(dynamicScraperService, "processedItemIdentifiers");
        assertNotNull(processedIdsAfterScrape);
        assertEquals(1, processedIdsAfterScrape.size(), "processedItemIdentifiers should contain the URL from the initial cachedScrapedData");
        assertTrue(processedIdsAfterScrape.contains(oldCachedUrl), "processedItemIdentifiers should have the old cached URL");

        // Verify Playwright resources are closed
        verify(mockBrowser).close();
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

    @Test
    void testScrapeStopsEarlyWhenOldUrlEncountered() {
        // 1. Setup processedItemIdentifiers with an "old" URL
        Set<String> knownOldUrls = new HashSet<>();
        String oldUrl = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/page1_item2_old.html";
        knownOldUrls.add(oldUrl);
        ReflectionTestUtils.setField(dynamicScraperService, "processedItemIdentifiers", knownOldUrls);

        // 2. Mock Playwright: page 0 has item1 (new), page 1 has item1 (new), item2 (old), item3 (new, but should not be reached)
        ReflectionTestUtils.setField(dynamicScraperService, "searchPagesSize", 1); // pages 0 and 1

        Locator mockH4sPage0 = mock(Locator.class, "H4sPage0");
        Locator mockH4sPage1 = mock(Locator.class, "H4sPage1");

        // Ensure navigate is stubbed for each expected URL if specific URLs are used in verification
        doNothing().when(mockPage).navigate(argThat(url -> url.endsWith("index.html")));
        doNothing().when(mockPage).navigate(argThat(url -> url.endsWith("index_1.html")));


        when(mockPage.locator(startsWith("xpath=")))
            .thenReturn(mockH4sPage0) // For first call after navigating to page 0
            .thenReturn(mockH4sPage1); // For second call after navigating to page 1

        // Page 0: 1 new item
        when(mockH4sPage0.count()).thenReturn(1);
        Locator h4Page0Item0 = mock(Locator.class, "h4Page0Item0");
        when(mockH4sPage0.nth(0)).thenReturn(h4Page0Item0);
        when(h4Page0Item0.textContent()).thenReturn("Page 0 Item 1 闵行");
        when(h4Page0Item0.locator("a").getAttribute("href")).thenReturn("page0_item1.html");

        // Page 1: item1 (new), item2 (OLD), item3 (new, after old)
        when(mockH4sPage1.count()).thenReturn(3);
        Locator h4Page1Item0 = mock(Locator.class, "h4Page1Item0"); // new
        Locator h4Page1Item1 = mock(Locator.class, "h4Page1Item1"); // old
        Locator h4Page1Item2 = mock(Locator.class, "h4Page1Item2"); // new, should not be processed

        when(mockH4sPage1.nth(0)).thenReturn(h4Page1Item0);
        when(h4Page1Item0.textContent()).thenReturn("Page 1 Item 1 闵行");
        when(h4Page1Item0.locator("a").getAttribute("href")).thenReturn("page1_item1_new.html");

        when(mockH4sPage1.nth(1)).thenReturn(h4Page1Item1);
        when(h4Page1Item1.textContent()).thenReturn("Page 1 Item 2 OLD 闵行");
        when(h4Page1Item1.locator("a").getAttribute("href")).thenReturn("page1_item2_old.html"); // This will resolve to oldUrl

        when(mockH4sPage1.nth(2)).thenReturn(h4Page1Item2);
        when(h4Page1Item2.textContent()).thenReturn("Page 1 Item 3 NEW 闵行");
        when(h4Page1Item2.locator("a").getAttribute("href")).thenReturn("page1_item3_new_after_old.html");
        
        // Call scrape
        List<ScrapedData> result = dynamicScraperService.scrape();

        // Assertions
        assertEquals(2, result.size(), "Should have 2 items: Page0/Item1 and Page1/Item1");
        assertEquals("Page 0 Item 1 闵行", result.get(0).text());
        assertTrue(result.get(0).url().endsWith("page0_item1.html"));
        assertEquals("Page 1 Item 1 闵行", result.get(1).text());
        assertTrue(result.get(1).url().endsWith("page1_item1_new.html"));
        
        // Verify that navigate was called for page 0 and page 1
        verify(mockPage).navigate(argThat(url -> url.endsWith("index.html") || url.contains(DynamicScraperService.BASE_URL + "index.html")));
        verify(mockPage).navigate(argThat(url -> url.endsWith("index_1.html") || url.contains(DynamicScraperService.BASE_URL + "index_1.html")));
        // Verify it did not attempt to navigate to further pages or re-navigate unnecessarily
        verify(mockPage, times(2)).navigate(anyString());


        // Verify items processed up to the old one
        verify(h4Page0Item0, times(1)).textContent();
        verify(h4Page1Item0, times(1)).textContent();
        verify(h4Page1Item1, times(1)).textContent(); // This is the "old" item, its textContent is read before the URL check
        verify(h4Page1Item2, never()).textContent(); // This item after the old one should not be processed
    }

    @Test
    void testScrapeFullWhenAllUrlsAreNewAndProcessedIdentifiersExist() {
        // 1. Pre-populate processedItemIdentifiers with some URLs that won't be encountered
        Set<String> knownOldUrls = new HashSet<>();
        knownOldUrls.add("https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/very_old_item.html");
        ReflectionTestUtils.setField(dynamicScraperService, "processedItemIdentifiers", knownOldUrls);

        ReflectionTestUtils.setField(dynamicScraperService, "searchPagesSize", 0); // Only page 0

        // Mock Playwright to return items whose URLs are *different* from processedItemIdentifiers
        when(mockH4sLocator.count()).thenReturn(2); // Two new items on page 0
        Locator h4Item0 = mock(Locator.class, "h4Item0");
        Locator h4Item1 = mock(Locator.class, "h4Item1");

        when(mockH4sLocator.nth(0)).thenReturn(h4Item0);
        when(h4Item0.textContent()).thenReturn("New Item 1 闵行");
        when(h4Item0.locator("a").getAttribute("href")).thenReturn("new_item1.html");

        when(mockH4sLocator.nth(1)).thenReturn(h4Item1);
        when(h4Item1.textContent()).thenReturn("New Item 2 闵行");
        when(h4Item1.locator("a").getAttribute("href")).thenReturn("new_item2.html");

        // Call scrape
        List<ScrapedData> result = dynamicScraperService.scrape();

        // Assertions
        assertEquals(2, result.size(), "Should contain all new items from page 0");
        assertEquals("New Item 1 闵行", result.get(0).text());
        assertTrue(result.get(0).url().endsWith("new_item1.html"));
        assertEquals("New Item 2 闵行", result.get(1).text());
        assertTrue(result.get(1).url().endsWith("new_item2.html"));

        // Verify navigation occurred for page 0
        verify(mockPage, times(1)).navigate(anyString());
    }

    @Test
    void testProcessedIdentifiersUpdatedCorrectly() {
        // 1. Set an initial state for cachedScrapedData (n-1 scrape results)
        List<ScrapedData> initialCachedItems = new ArrayList<>();
        String oldUrl1 = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/old_url1.html";
        initialCachedItems.add(new ScrapedData("Old Text 1", oldUrl1, "old_date1"));
        String oldUrl2 = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/old_url2.html";
        initialCachedItems.add(new ScrapedData("Old Text 2", oldUrl2, "old_date2"));
        ReflectionTestUtils.setField(dynamicScraperService, "cachedScrapedData", new ArrayList<>(initialCachedItems));
        ReflectionTestUtils.setField(dynamicScraperService, "processedItemIdentifiers", new HashSet<>());

        // 2. Mock Playwright to return a *new* list of items for the current scrape (nth scrape)
        String newHref = "new_url.html";
        String newAbsUrl = "https://hd.ghzyj.sh.gov.cn/2017/zdxxgk/" + newHref;

        when(mockH4sLocator.count()).thenReturn(1);
        when(mockH4sLocator.nth(0)).thenReturn(mockH4Locator);
        when(mockH4Locator.textContent()).thenReturn("New Text 闵行");
        when(mockH4Locator.locator("a").getAttribute("href")).thenReturn(newHref);

        // 3. Call dynamicScraperService.scrape()
        List<ScrapedData> resultFromScrape = dynamicScraperService.scrape();

        // 4. Assert processedItemIdentifiers
        @SuppressWarnings("unchecked")
        Set<String> processedIds = (Set<String>) ReflectionTestUtils.getField(dynamicScraperService, "processedItemIdentifiers");
        assertNotNull(processedIds);
        assertEquals(2, processedIds.size(), "processedItemIdentifiers should contain URLs from the initial cachedScrapedData");
        assertTrue(processedIds.contains(oldUrl1));
        assertTrue(processedIds.contains(oldUrl2));

        // 5. Assert the new cache state (getLatestScrapedData)
        List<ScrapedData> currentCache = dynamicScraperService.getLatestScrapedData();
        assertNotNull(currentCache);
        assertEquals(1, currentCache.size(), "Cache should now contain only the newly scraped item");
        assertEquals("New Text 闵行", currentCache.get(0).text());
        assertEquals(newAbsUrl, currentCache.get(0).url());

        // 6. Assert the returned list from scrape() is the new data
        assertEquals(1, resultFromScrape.size());
        assertEquals(newAbsUrl, resultFromScrape.get(0).url());
    }
}
