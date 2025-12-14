package com.searchlocal.model;

import javax.persistence.*;

@Entity
@Table(name = "page", indexes = @javax.persistence.Index(name = "idx_path", columnList = "path"))
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;
    
    @Column(columnDefinition = "VARCHAR(500)")
    private String path;
    
    private Integer code;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    public Page() {
    }
    
    public Page(Site site, String path, Integer code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Site getSite() {
        return site;
    }
    
    public void setSite(Site site) {
        this.site = site;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
}

