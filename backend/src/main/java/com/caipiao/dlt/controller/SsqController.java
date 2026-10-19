package com.caipiao.dlt.controller;

import com.caipiao.dlt.dto.ApiResponse;
import com.caipiao.dlt.dto.FetchResult;
import com.caipiao.dlt.dto.RecommendResponse;
import com.caipiao.dlt.dto.StatisticsInfo;
import com.caipiao.dlt.entity.SsqNumber;
import com.caipiao.dlt.repository.SsqNumberRepository;
import com.caipiao.dlt.service.SsqCrawlService;
import com.caipiao.dlt.service.SsqRecommendService;
import com.caipiao.dlt.service.SsqStatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ssq")
public class SsqController {

    private final SsqCrawlService crawlService;
    private final SsqRecommendService recommendService;
    private final SsqStatisticsService statisticsService;
    private final SsqNumberRepository repository;

    public SsqController(SsqCrawlService crawlService,
                         SsqRecommendService recommendService,
                         SsqStatisticsService statisticsService,
                         SsqNumberRepository repository) {
        this.crawlService = crawlService;
        this.recommendService = recommendService;
        this.statisticsService = statisticsService;
        this.repository = repository;
    }

    @PostMapping("/fetch")
    public ApiResponse<FetchResult> fetch(@RequestParam(value = "count", defaultValue = "150") int count) {
        try {
            return ApiResponse.ok(crawlService.fetchLatest(count));
        } catch (Exception e) {
            return ApiResponse.error("拉取双色球数据失败：" + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ApiResponse<List<SsqNumber>> history(@RequestParam(value = "limit", defaultValue = "100") int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        return ApiResponse.ok(repository.findRecent(PageRequest.of(0, size)));
    }

    @GetMapping("/statistics")
    public ApiResponse<StatisticsInfo> statistics() {
        return ApiResponse.ok(statisticsService.computeStatistics());
    }

    @GetMapping("/recommend")
    public ApiResponse<RecommendResponse> recommend() {
        try {
            return ApiResponse.ok(recommendService.generate());
        } catch (Exception e) {
            return ApiResponse.error("生成推荐失败：" + e.getMessage());
        }
    }
}
