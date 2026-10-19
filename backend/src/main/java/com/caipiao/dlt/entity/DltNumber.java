package com.caipiao.dlt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * 大乐透开奖号码实体
 */
@Entity
@Table(name = "dlt_number")
public class DltNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue", nullable = false, unique = true, length = 16)
    private String issue;

    @Column(name = "front1", nullable = false)
    private Integer front1;

    @Column(name = "front2", nullable = false)
    private Integer front2;

    @Column(name = "front3", nullable = false)
    private Integer front3;

    @Column(name = "front4", nullable = false)
    private Integer front4;

    @Column(name = "front5", nullable = false)
    private Integer front5;

    @Column(name = "back1", nullable = false)
    private Integer back1;

    @Column(name = "back2", nullable = false)
    private Integer back2;

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

    public DltNumber() {
    }

    public DltNumber(String issue, int[] fronts, int[] backs, LocalDate drawDate) {
        this.issue = issue;
        this.front1 = fronts[0];
        this.front2 = fronts[1];
        this.front3 = fronts[2];
        this.front4 = fronts[3];
        this.front5 = fronts[4];
        this.back1 = backs[0];
        this.back2 = backs[1];
        this.drawDate = drawDate;
    }

    public int[] fronts() {
        return new int[]{front1, front2, front3, front4, front5};
    }

    public int[] backs() {
        return new int[]{back1, back2};
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public Integer getFront1() { return front1; }
    public void setFront1(Integer front1) { this.front1 = front1; }

    public Integer getFront2() { return front2; }
    public void setFront2(Integer front2) { this.front2 = front2; }

    public Integer getFront3() { return front3; }
    public void setFront3(Integer front3) { this.front3 = front3; }

    public Integer getFront4() { return front4; }
    public void setFront4(Integer front4) { this.front4 = front4; }

    public Integer getFront5() { return front5; }
    public void setFront5(Integer front5) { this.front5 = front5; }

    public Integer getBack1() { return back1; }
    public void setBack1(Integer back1) { this.back1 = back1; }

    public Integer getBack2() { return back2; }
    public void setBack2(Integer back2) { this.back2 = back2; }

    public LocalDate getDrawDate() { return drawDate; }
    public void setDrawDate(LocalDate drawDate) { this.drawDate = drawDate; }
}
