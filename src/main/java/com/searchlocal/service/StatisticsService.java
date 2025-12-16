package com.searchlocal.service;

import com.searchlocal.dto.StatisticsResponse;
import com.searchlocal.model.*;
import com.searchlocal.repository.*;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticsService {
    
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    
    public StatisticsService(
            SiteRepository siteRepository,
            PageRepository pageRepository,
            LemmaRepository lemmaRepository,
            IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }
    
    public StatisticsResponse getStatistics() {
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
                .map(this::mapSiteToDetailedStatistics)
                .collect(Collectors.toList());
        
        return new StatisticsResponse(total, detailed);
    }
    
    private StatisticsResponse.DetailedStatisticsItem mapSiteToDetailedStatistics(Site site) {
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
        
        long lemmasCount = indexRepository.findAll().stream()
                .filter(idx -> idx.getPage().getSite().equals(site))
                .map(idx -> idx.getLemma().getId())
                .distinct()
                .count();
        item.setLemmas((int) lemmasCount);
        
        return item;
    }
}
