package com.caipiao.dlt.repository;

import com.caipiao.dlt.entity.DltNumber;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DltNumberRepository extends JpaRepository<DltNumber, Long> {

    Optional<DltNumber> findByIssue(String issue);

    boolean existsByIssue(String issue);

    long count();

    /**
     * 按开奖日期降序、id 降序取最近 N 期（用于冷号统计与重号计算）
     */
    @Query("select d from DltNumber d order by d.drawDate desc, d.id desc")
    List<DltNumber> findRecent(Pageable pageable);
}
