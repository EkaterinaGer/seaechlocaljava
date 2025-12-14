package com.searchlocal.web;

import com.searchlocal.dto.StatisticsResponse;
import com.searchlocal.model.*;
import com.searchlocal.repository.*;
import com.searchlocal.service.CrawlingService;
import com.searchlocal.service.IndexingService;
import com.searchlocal.service.SearchService;
import com.searchlocal.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    
    @Autowired
    private CrawlingService crawlingService;
    
    @Autowired
    private IndexingService indexingService;
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private SiteRepository siteRepository;
    
    @Autowired
    private PageRepository pageRepository;
    
    @Autowired
    private LemmaRepository lemmaRepository;
    
    @Autowired
    private IndexRepository indexRepository;
    
    /**
     * Запуск индексации сайта
     */
    @PostMapping("/startIndexing")
    public ResponseEntity<Map<String, Object>> startIndexing(@RequestParam String url) {
        try {
            crawlingService.startIndexing(url);
            return ResponseEntity.ok(Map.of("result", true));
        } catch (Exception e) {
            logger.error("Ошибка при запуске индексации: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("result", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Остановка индексации
     */
    @PostMapping("/stopIndexing")
    public ResponseEntity<Map<String, Object>> stopIndexing() {
        try {
            crawlingService.stopIndexing();
            return ResponseEntity.ok(Map.of("result", true));
        } catch (Exception e) {
            logger.error("Ошибка при остановке индексации: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("result", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Индексация отдельной страницы
     */
    @PostMapping("/indexPage")
    public ResponseEntity<Map<String, Object>> indexPage(@RequestParam String url) {
        try {
            // Определяем сайт по URL
            String siteUrl = extractSiteUrl(url);
            indexingService.indexPage(url, siteUrl);
            return ResponseEntity.ok(Map.of("result", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("result", false, "error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Ошибка при индексации страницы: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("result", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Поиск
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(required = false) String site) {
        try {
            List<SearchResult> results = searchService.search(query, site);
            return ResponseEntity.ok(Map.of(
                    "result", true,
                    "count", results.size(),
                    "data", results
            ));
        } catch (Exception e) {
            logger.error("Ошибка при поиске: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("result", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Статистика
     */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        List<Site> sites = siteRepository.findAll();
        long totalPages = pageRepository.count();
        long totalLemmas = lemmaRepository.count();
        boolean isIndexing = sites.stream()
                .anyMatch(s -> s.getStatus() == SiteStatus.INDEXING);
        
        StatisticsResponse.TotalStatistics total = new StatisticsResponse.TotalStatistics(
                sites.size(),
                (int) totalPages,
                (int) totalLemmas,
                isIndexing
        );
        
        List<StatisticsResponse.DetailedStatisticsItem> detailed = sites.stream()
                .map(site -> {
                    StatisticsResponse.DetailedStatisticsItem item = new StatisticsResponse.DetailedStatisticsItem();
                    item.setUrl(site.getUrl());
                    item.setName(site.getName());
                    item.setStatus(site.getStatus().name());
                    item.setStatusTime(site.getStatusTime() != null ?
                            site.getStatusTime().toEpochSecond(ZoneOffset.UTC) : 0);
                    item.setLastError(site.getLastError());
                    
                    long pagesCount = pageRepository.findAll().stream()
                            .filter(p -> p.getSite().equals(site))
                            .count();
                    item.setPages((int) pagesCount);
                    
                    // Подсчитываем уникальные леммы для сайта
                    long lemmasCount = indexRepository.findAll().stream()
                            .filter(idx -> idx.getPage().getSite().equals(site))
                            .map(idx -> idx.getLemma().getId())
                            .distinct()
                            .count();
                    item.setLemmas((int) lemmasCount);
                    
                    return item;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(new StatisticsResponse(total, detailed));
    }
    
    private String extractSiteUrl(String pageUrl) {
        try {
            java.net.URI uri = new java.net.URI(pageUrl);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный URL");
        }
    }
}

