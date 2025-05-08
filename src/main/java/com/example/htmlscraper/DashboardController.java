package com.example.htmlscraper;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ScraperService scraperService;

    public DashboardController(ScraperService scraperService) {
        this.scraperService = scraperService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("links", scraperService.getMatchedLinks());
        return "dashboard";
    }
}
