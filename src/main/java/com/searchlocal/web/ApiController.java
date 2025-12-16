package com.searchlocal.web;

import com.searchlocal.dto.StatisticsResponse;
import com.searchlocal.model.SearchResult;
import com.searchlocal.service.CrawlingService;
import com.searchlocal.service.IndexingService;
import com.searchlocal.service.SearchService;
import com.searchlocal.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    
    private final CrawlingService crawlingService;
    private final IndexingService indexingService;
    private final SearchService searchService;
    private final StatisticsService statisticsService;
    
    public ApiController(
            CrawlingService crawlingService,
            IndexingService indexingService,
            SearchService searchService,
            StatisticsService statisticsService) {
        this.crawlingService = crawlingService;
        this.indexingService = indexingService;
        this.searchService = searchService;
        this.statisticsService = statisticsService;
    }
    
    @PostMapping("/startIndexing")
    public Map<String, Object> startIndexing(@RequestParam String url) {
        crawlingService.startIndexing(url);
        return Map.of("result", true);
    }
    
    @PostMapping("/stopIndexing")
    public Map<String, Object> stopIndexing() {
        crawlingService.stopIndexing();
        return Map.of("result", true);
    }
    
    @PostMapping("/indexPage")
    public Map<String, Object> indexPage(@RequestParam String url) {
        indexingService.indexPage(url);
        return Map.of("result", true);
    }
    
    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam String query,
            @RequestParam(required = false) String site) {
        List<SearchResult> results = searchService.search(query, site);
        return Map.of(
                "result", true,
                "count", results.size(),
                "data", results
        );
    }
    
    @GetMapping("/statistics")
    public StatisticsResponse statistics() {
        return statisticsService.getStatistics();
    }
}

