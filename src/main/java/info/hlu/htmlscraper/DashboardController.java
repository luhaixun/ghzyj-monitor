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
        // Add the map to the model for Thymeleaf
        model.addAttribute("links", scraperService.getMatchedLinks());
        model.addAttribute("linksDate", scraperService.getLinksDate());
        return "dashboard";
    }
}
