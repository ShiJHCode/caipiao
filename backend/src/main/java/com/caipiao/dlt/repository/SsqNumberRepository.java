package com.caipiao.dlt.repository;

import com.caipiao.dlt.entity.SsqNumber;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SsqNumberRepository extends JpaRepository<SsqNumber, Long> {

    Optional<SsqNumber> findByIssue(String issue);

    boolean existsByIssue(String issue);

    long count();

    @Query("select s from SsqNumber s order by s.drawDate desc, s.id desc")
    List<SsqNumber> findRecent(Pageable pageable);
}
