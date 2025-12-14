package com.searchlocal.repository;

import com.searchlocal.model.Site;
import com.searchlocal.model.SiteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    Optional<Site> findByUrl(String url);
    List<Site> findByStatus(SiteStatus status);
}

