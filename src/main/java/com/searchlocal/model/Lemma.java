package com.searchlocal.model;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(columnDefinition = "VARCHAR(255)", unique = true)
    private String lemma;
    
    private Integer frequency;
    
    public Lemma() {
    }
    
    public Lemma(String lemma, Integer frequency) {
        this.lemma = lemma;
        this.frequency = frequency;
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getLemma() {
        return lemma;
    }
    
    public void setLemma(String lemma) {
        this.lemma = lemma;
    }
    
    public Integer getFrequency() {
        return frequency;
    }
    
    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }
}

