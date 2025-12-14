package com.searchlocal.repository;

import com.searchlocal.model.Index;
import com.searchlocal.model.Lemma;
import com.searchlocal.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    List<Index> findByLemma(Lemma lemma);
    List<Index> findByPage(Page page);
}

