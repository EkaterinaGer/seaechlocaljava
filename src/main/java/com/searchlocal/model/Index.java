package com.searchlocal.model;

import javax.persistence.*;

@Entity
@Table(name = "index")
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;
    
    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;
    
    @Column(name = "rank")
    private Float rank;
    
    public Index() {
    }
    
    public Index(Page page, Lemma lemma, Float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Page getPage() {
        return page;
    }
    
    public void setPage(Page page) {
        this.page = page;
    }
    
    public Lemma getLemma() {
        return lemma;
    }
    
    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }
    
    public Float getRank() {
        return rank;
    }
    
    public void setRank(Float rank) {
        this.rank = rank;
    }
}

