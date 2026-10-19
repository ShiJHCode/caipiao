package com.caipiao.dlt.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史统计信息（实际统计 vs 理论值）
 */
public class StatisticsInfo {

    private int sampleSize;                              // 统计样本期数
    private List<StatItem> zoneStats = new ArrayList<>();        // 空间分布
    private List<StatItem> oddEvenStats = new ArrayList<>();     // 前区奇偶
    private List<StatItem> spanStats = new ArrayList<>();        // 跨度
    private List<StatItem> repeatStats = new ArrayList<>();      // 重号
    private List<StatItem> consecutiveStats = new ArrayList<>(); // 连号
    private List<StatItem> backOddEvenStats = new ArrayList<>(); // 后区奇偶
    private List<FreqItem> frontFreq = new ArrayList<>();        // 前区号码频次（升序=冷号在前）
    private List<FreqItem> backFreq = new ArrayList<>();         // 后区号码频次

    public static class StatItem {
        private String name;
        private int count;
        private double actualRatio;
        private double theoryRatio;
        private double theoryPeriod;
        private int omit;              // 当前遗漏期数（距上次出现已过去多少期）
        private boolean expecting;     // 是否处于"欠回归"状态（遗漏 >= 理论回归周期）

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getActualRatio() { return actualRatio; }
        public void setActualRatio(double actualRatio) { this.actualRatio = actualRatio; }
        public double getTheoryRatio() { return theoryRatio; }
        public void setTheoryRatio(double theoryRatio) { this.theoryRatio = theoryRatio; }
        public double getTheoryPeriod() { return theoryPeriod; }
        public void setTheoryPeriod(double theoryPeriod) { this.theoryPeriod = theoryPeriod; }
        public int getOmit() { return omit; }
        public void setOmit(int omit) { this.omit = omit; }
        public boolean isExpecting() { return expecting; }
        public void setExpecting(boolean expecting) { this.expecting = expecting; }
    }

    public static class FreqItem {
        private int number;
        private int freq;
        private int omit;   // 距最近一次出现的遗漏期数（0=上一期刚出现）

        public FreqItem(int number, int freq, int omit) {
            this.number = number;
            this.freq = freq;
            this.omit = omit;
        }

        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        public int getFreq() { return freq; }
        public void setFreq(int freq) { this.freq = freq; }
        public int getOmit() { return omit; }
        public void setOmit(int omit) { this.omit = omit; }
    }

    public int getSampleSize() { return sampleSize; }
    public void setSampleSize(int sampleSize) { this.sampleSize = sampleSize; }

    public List<StatItem> getZoneStats() { return zoneStats; }
    public void setZoneStats(List<StatItem> zoneStats) { this.zoneStats = zoneStats; }

    public List<StatItem> getOddEvenStats() { return oddEvenStats; }
    public void setOddEvenStats(List<StatItem> oddEvenStats) { this.oddEvenStats = oddEvenStats; }

    public List<StatItem> getSpanStats() { return spanStats; }
    public void setSpanStats(List<StatItem> spanStats) { this.spanStats = spanStats; }

    public List<StatItem> getRepeatStats() { return repeatStats; }
    public void setRepeatStats(List<StatItem> repeatStats) { this.repeatStats = repeatStats; }

    public List<StatItem> getConsecutiveStats() { return consecutiveStats; }
    public void setConsecutiveStats(List<StatItem> consecutiveStats) { this.consecutiveStats = consecutiveStats; }

    public List<StatItem> getBackOddEvenStats() { return backOddEvenStats; }
    public void setBackOddEvenStats(List<StatItem> backOddEvenStats) { this.backOddEvenStats = backOddEvenStats; }

    public List<FreqItem> getFrontFreq() { return frontFreq; }
    public void setFrontFreq(List<FreqItem> frontFreq) { this.frontFreq = frontFreq; }

    public List<FreqItem> getBackFreq() { return backFreq; }
    public void setBackFreq(List<FreqItem> backFreq) { this.backFreq = backFreq; }
}
