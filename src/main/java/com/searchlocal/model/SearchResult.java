package com.searchlocal.model;

/**
 * Результат поиска
 */
public class SearchResult implements Comparable<SearchResult> {
    private String url;
    private String title;
    private String snippet;
    private double relevance;

    public SearchResult(String url, String title, String snippet, double relevance) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.relevance = relevance;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public double getRelevance() {
        return relevance;
    }

    @Override
    public int compareTo(SearchResult other) {
        return Double.compare(other.relevance, this.relevance);
    }

    @Override
    public String toString() {
        return String.format("SearchResult{url='%s', title='%s', relevance=%.4f}", 
                url, title, relevance);
    }
}

