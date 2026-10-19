package com.caipiao.dlt.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * 双色球开奖号码实体（红球 6 码 1-33 + 蓝球 1 码 1-16）
 */
@Entity
@Table(name = "ssq_number")
public class SsqNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue", nullable = false, unique = true, length = 16)
    private String issue;

    @Column(name = "red1", nullable = false)
    private Integer red1;
    @Column(name = "red2", nullable = false)
    private Integer red2;
    @Column(name = "red3", nullable = false)
    private Integer red3;
    @Column(name = "red4", nullable = false)
    private Integer red4;
    @Column(name = "red5", nullable = false)
    private Integer red5;
    @Column(name = "red6", nullable = false)
    private Integer red6;

    @Column(name = "blue", nullable = false)
    private Integer blue;

    @Column(name = "draw_date", nullable = false)
    private LocalDate drawDate;

    public SsqNumber() {
    }

    public SsqNumber(String issue, int[] reds, int blue, LocalDate drawDate) {
        this.issue = issue;
        this.red1 = reds[0];
        this.red2 = reds[1];
        this.red3 = reds[2];
        this.red4 = reds[3];
        this.red5 = reds[4];
        this.red6 = reds[5];
        this.blue = blue;
        this.drawDate = drawDate;
    }

    public int[] reds() {
        return new int[]{red1, red2, red3, red4, red5, red6};
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }

    public Integer getRed1() { return red1; }
    public void setRed1(Integer red1) { this.red1 = red1; }
    public Integer getRed2() { return red2; }
    public void setRed2(Integer red2) { this.red2 = red2; }
    public Integer getRed3() { return red3; }
    public void setRed3(Integer red3) { this.red3 = red3; }
    public Integer getRed4() { return red4; }
    public void setRed4(Integer red4) { this.red4 = red4; }
    public Integer getRed5() { return red5; }
    public void setRed5(Integer red5) { this.red5 = red5; }
    public Integer getRed6() { return red6; }
    public void setRed6(Integer red6) { this.red6 = red6; }

    public Integer getBlue() { return blue; }
    public void setBlue(Integer blue) { this.blue = blue; }

    public LocalDate getDrawDate() { return drawDate; }
    public void setDrawDate(LocalDate drawDate) { this.drawDate = drawDate; }
}
