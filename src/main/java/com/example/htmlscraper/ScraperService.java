package com.example.htmlscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ScraperService {

    private static final String BASE_URL="https://hd.ghzyj.sh.gov.cn/2017/zdxxgk";
    private static final int URLS= 10;

    private static final String KEYWORD = "闵行";

    private final List<String> matchedLinks = new CopyOnWriteArrayList<>();

    public List<String> getMatchedLinks() {
        return List.copyOf(matchedLinks);
    }

    @Scheduled(fixedRate = 1000 * 60 * 60 * 12) // Every 12 hours
    public void scrape() {
        matchedLinks.clear();
        for(int i=0;i<URLS;i++){
            String url;
            if(i==0){
                    url=BASE_URL+"/index.html";
            }else{
                url =BASE_URL+"/index_"+i+".html";
            }
            try {
                Document doc = Jsoup.connect(url).get();
                Elements links = doc.select("a[title]");
                for (Element link : links) {
                    if (link.text().contains(KEYWORD)) {
                        matchedLinks.add(link.text() + " - " + link.absUrl("href"));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error fetching " + url + ": " + e.getMessage());
            }
        }
    }
}
