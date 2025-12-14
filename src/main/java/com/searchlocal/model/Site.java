package com.searchlocal.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    private SiteStatus status;
    
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @Column(columnDefinition = "VARCHAR(255)")
    private String url;
    
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;
    
    public Site() {
    }
    
    public Site(String url, String name) {
        this.url = url;
        this.name = name;
        this.status = SiteStatus.INDEXING;
        this.statusTime = LocalDateTime.now();
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public SiteStatus getStatus() {
        return status;
    }
    
    public void setStatus(SiteStatus status) {
        this.status = status;
        this.statusTime = LocalDateTime.now();
    }
    
    public LocalDateTime getStatusTime() {
        return statusTime;
    }
    
    public void setStatusTime(LocalDateTime statusTime) {
        this.statusTime = statusTime;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String lastError) {
        this.lastError = lastError;
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
}

