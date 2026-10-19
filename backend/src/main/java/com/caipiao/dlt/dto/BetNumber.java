package com.caipiao.dlt.dto;

import java.util.List;

/**
 * 一注推荐号码
 */
public class BetNumber {

    private int index;                 // 第几注（1-5）
    private String strategy;           // 策略说明
    private List<Integer> fronts;      // 前区 5 码（升序）
    private List<Integer> backs;       // 后区 2 码（升序）
    private String oddEven;            // 前区奇偶分布，如 "3奇2偶"
    private int span;                  // 前区跨度 = 最大值 - 最小值
    private String zoneDesc;           // 三区分布描述
    private boolean hasConsecutive;    // 是否含连号
    private List<String> tags;         // 命中的回归特征标签

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public List<Integer> getFronts() { return fronts; }
    public void setFronts(List<Integer> fronts) { this.fronts = fronts; }

    public List<Integer> getBacks() { return backs; }
    public void setBacks(List<Integer> backs) { this.backs = backs; }

    public String getOddEven() { return oddEven; }
    public void setOddEven(String oddEven) { this.oddEven = oddEven; }

    public int getSpan() { return span; }
    public void setSpan(int span) { this.span = span; }

    public String getZoneDesc() { return zoneDesc; }
    public void setZoneDesc(String zoneDesc) { this.zoneDesc = zoneDesc; }

    public boolean isHasConsecutive() { return hasConsecutive; }
    public void setHasConsecutive(boolean hasConsecutive) { this.hasConsecutive = hasConsecutive; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
