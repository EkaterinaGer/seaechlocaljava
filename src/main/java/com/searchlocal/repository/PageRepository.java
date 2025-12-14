package com.searchlocal.repository;

import com.searchlocal.model.Page;
import com.searchlocal.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findBySiteAndPath(Site site, String path);
    boolean existsBySiteAndPath(Site site, String path);
}

