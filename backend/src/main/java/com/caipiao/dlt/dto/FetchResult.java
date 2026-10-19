package com.caipiao.dlt.dto;

/**
 * 爬取开奖数据结果
 */
public class FetchResult {

    private int insertedCount;   // 本次新增入库条数
    private int totalCount;      // 数据库当前总条数
    private String latestIssue;  // 数据库中最新的期号
    private String latestDate;   // 最新开奖日期

    public int getInsertedCount() { return insertedCount; }
    public void setInsertedCount(int insertedCount) { this.insertedCount = insertedCount; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public String getLatestIssue() { return latestIssue; }
    public void setLatestIssue(String latestIssue) { this.latestIssue = latestIssue; }

    public String getLatestDate() { return latestDate; }
    public void setLatestDate(String latestDate) { this.latestDate = latestDate; }
}
