package info.hlu.htmlscraper;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DynamicScraperService scraperService;

    public DashboardController(DynamicScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // Call scraperService.getLatestScrapedData() to get the List<ScrapedData>
        java.util.List<ScrapedData> scrapedDataList = scraperService.getLatestScrapedData();
        // Add the list of ScrapedData objects to the model
        model.addAttribute("scrapedItems", scrapedDataList);
        return "dashboard";
    }
}
