package com.caipiao.dlt.dto;

import java.util.List;

/**
 * 推荐结果
 */
public class RecommendResponse {

    private List<BetNumber> bets;          // 5 注推荐号码
    private List<Integer> coldFronts;      // 前区冷号（频次升序）
    private List<Integer> coldBacks;       // 后区冷号
    private int dataWindow;                // 统计窗口期数
    private StatisticsInfo statistics;     // 历史统计 vs 理论值

    public List<BetNumber> getBets() { return bets; }
    public void setBets(List<BetNumber> bets) { this.bets = bets; }

    public List<Integer> getColdFronts() { return coldFronts; }
    public void setColdFronts(List<Integer> coldFronts) { this.coldFronts = coldFronts; }

    public List<Integer> getColdBacks() { return coldBacks; }
    public void setColdBacks(List<Integer> coldBacks) { this.coldBacks = coldBacks; }

    public int getDataWindow() { return dataWindow; }
    public void setDataWindow(int dataWindow) { this.dataWindow = dataWindow; }

    public StatisticsInfo getStatistics() { return statistics; }
    public void setStatistics(StatisticsInfo statistics) { this.statistics = statistics; }
}
