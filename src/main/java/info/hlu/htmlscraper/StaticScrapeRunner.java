package info.hlu.htmlscraper;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StaticScrapeRunner implements ApplicationRunner {

    private final DynamicScraperService scraperService;

    public StaticScrapeRunner(DynamicScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @Override
    public void run(ApplicationArguments args) {
        scraperService.scrape(); // runs and writes static dashboard
    }
}
