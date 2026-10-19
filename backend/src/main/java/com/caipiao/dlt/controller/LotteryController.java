package com.caipiao.dlt.controller;

import com.caipiao.dlt.dto.ApiResponse;
import com.caipiao.dlt.dto.FetchResult;
import com.caipiao.dlt.dto.RecommendResponse;
import com.caipiao.dlt.dto.StatisticsInfo;
import com.caipiao.dlt.entity.DltNumber;
import com.caipiao.dlt.repository.DltNumberRepository;
import com.caipiao.dlt.service.CrawlService;
import com.caipiao.dlt.service.RecommendService;
import com.caipiao.dlt.service.StatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lottery")
public class LotteryController {

    private final CrawlService crawlService;
    private final RecommendService recommendService;
    private final StatisticsService statisticsService;
    private final DltNumberRepository repository;

    public LotteryController(CrawlService crawlService,
                             RecommendService recommendService,
                             StatisticsService statisticsService,
                             DltNumberRepository repository) {
        this.crawlService = crawlService;
        this.recommendService = recommendService;
        this.statisticsService = statisticsService;
        this.repository = repository;
    }

    /**
     * 拉取最新开奖数据（首次默认 150 期，之后增量）
     */
    @PostMapping("/fetch")
    public ApiResponse<FetchResult> fetch(@RequestParam(value = "count", defaultValue = "150") int count) {
        try {
            FetchResult result = crawlService.fetchLatest(count);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.error("拉取开奖数据失败：" + e.getMessage());
        }
    }

    /**
     * 查询历史开奖数据（按开奖日期降序，最多返回 limit 条）
     */
    @GetMapping("/history")
    public ApiResponse<List<DltNumber>> history(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        List<DltNumber> list = repository.findRecent(PageRequest.of(0, size));
        return ApiResponse.ok(list);
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public ApiResponse<StatisticsInfo> statistics() {
        return ApiResponse.ok(statisticsService.computeStatistics());
    }

    /**
     * 生成 5 注推荐号码
     */
    @GetMapping("/recommend")
    public ApiResponse<RecommendResponse> recommend() {
        try {
            return ApiResponse.ok(recommendService.generate());
        } catch (Exception e) {
            return ApiResponse.error("生成推荐失败：" + e.getMessage());
        }
    }
}
