package info.hlu.htmlscraper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List; // Make sure this is imported

@Service
@Slf4j
public class ScheduledScraperService {

    private final DynamicScraperService dynamicScraperService;
    private final StaticHtmlWriterService staticHtmlWriterService; // Added

    public ScheduledScraperService(DynamicScraperService dynamicScraperService,
                                   StaticHtmlWriterService staticHtmlWriterService) { // Added
        this.dynamicScraperService = dynamicScraperService;
        this.staticHtmlWriterService = staticHtmlWriterService; // Added
    }

    @Scheduled(cron = "0 0 9,18 * * *", zone = "Asia/Shanghai")
    public void performScrapingTask() {
        log.info("Starting scheduled scrape task.");
        List<ScrapedData> scrapedData = dynamicScraperService.scrape(); // Ensure scrape returns the list
        log.info("Scheduled scrape task completed. Found {} items.", scrapedData.size());
        
        log.info("Starting static HTML generation.");
        staticHtmlWriterService.writeDashboardHtml(scrapedData, "docs/dashboard.html");
        log.info("Static HTML generation completed.");
    }
}
