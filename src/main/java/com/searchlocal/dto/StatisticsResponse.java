package com.searchlocal.dto;

import java.util.List;

public class StatisticsResponse {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;
    
    public StatisticsResponse() {
    }
    
    public StatisticsResponse(TotalStatistics total, List<DetailedStatisticsItem> detailed) {
        this.total = total;
        this.detailed = detailed;
    }
    
    public TotalStatistics getTotal() {
        return total;
    }
    
    public void setTotal(TotalStatistics total) {
        this.total = total;
    }
    
    public List<DetailedStatisticsItem> getDetailed() {
        return detailed;
    }
    
    public void setDetailed(List<DetailedStatisticsItem> detailed) {
        this.detailed = detailed;
    }
    
    public static class TotalStatistics {
        private int sites;
        private int pages;
        private int lemmas;
        private boolean isIndexing;
        
        public TotalStatistics() {
        }
        
        public TotalStatistics(int sites, int pages, int lemmas, boolean isIndexing) {
            this.sites = sites;
            this.pages = pages;
            this.lemmas = lemmas;
            this.isIndexing = isIndexing;
        }
        
        public int getSites() {
            return sites;
        }
        
        public void setSites(int sites) {
            this.sites = sites;
        }
        
        public int getPages() {
            return pages;
        }
        
        public void setPages(int pages) {
            this.pages = pages;
        }
        
        public int getLemmas() {
            return lemmas;
        }
        
        public void setLemmas(int lemmas) {
            this.lemmas = lemmas;
        }
        
        public boolean isIndexing() {
            return isIndexing;
        }
        
        public void setIndexing(boolean indexing) {
            isIndexing = indexing;
        }
    }
    
    public static class DetailedStatisticsItem {
        private String url;
        private String name;
        private String status;
        private long statusTime;
        private String lastError;
        private int pages;
        private int lemmas;
        
        public DetailedStatisticsItem() {
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public long getStatusTime() {
            return statusTime;
        }
        
        public void setStatusTime(long statusTime) {
            this.statusTime = statusTime;
        }
        
        public String getLastError() {
            return lastError;
        }
        
        public void setLastError(String lastError) {
            this.lastError = lastError;
        }
        
        public int getPages() {
            return pages;
        }
        
        public void setPages(int pages) {
            this.pages = pages;
        }
        
        public int getLemmas() {
            return lemmas;
        }
        
        public void setLemmas(int lemmas) {
            this.lemmas = lemmas;
        }
    }
}

