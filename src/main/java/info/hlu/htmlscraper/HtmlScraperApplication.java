package info.hlu.htmlscraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HtmlScraperApplication {
    public static void main(String[] args) {
        SpringApplication.run(HtmlScraperApplication.class, args);
    }
}
