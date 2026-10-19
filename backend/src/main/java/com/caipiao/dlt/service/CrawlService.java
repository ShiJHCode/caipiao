package com.caipiao.dlt.service;

import com.caipiao.dlt.dto.FetchResult;
import com.caipiao.dlt.entity.DltNumber;
import com.caipiao.dlt.repository.DltNumberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 大乐透历史开奖数据爬取服务
 * 数据源：中国体育彩票官网接口（gameNo=85 为大乐透）
 */
@Service
public class CrawlService {

    private static final Logger log = LoggerFactory.getLogger(CrawlService.class);

    private final DltNumberRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${lottery.crawl.url}")
    private String crawlUrl;

    @Value("${lottery.crawl.game-no}")
    private int gameNo;

    @Value("${lottery.crawl.page-size:100}")
    private int pageSize;

    @Value("${lottery.crawl.default-count:150}")
    private int defaultCount;

    @Value("${lottery.crawl.read-timeout:15000}")
    private int readTimeout;

    public CrawlService(DltNumberRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(10000))
                .build();
    }

    /**
     * 增量拉取最新开奖数据（默认 150 期）
     *
     * @param count 期望拉取的最新期数，<=0 时使用默认值
     */
    public FetchResult fetchLatest(int count) {
        int target = count > 0 ? count : defaultCount;
        int inserted = 0;
        int pageNo = 1;
        // 最多翻 target/pageSize + 2 页，防止异常死循环
        int maxPages = Math.max(1, (target + pageSize - 1) / pageSize) + 2;

        while (pageNo <= maxPages) {
            List<DltNumber> page = fetchPage(pageNo);
            if (page == null || page.isEmpty()) {
                break;
            }
            boolean pageAllExist = true;
            for (DltNumber item : page) {
                if (repository.existsByIssue(item.getIssue())) {
                    continue;
                }
                repository.save(item);
                inserted++;
                pageAllExist = false;
            }
            // 若整页都已存在，说明后续更旧且均已入库，提前结束
            if (pageAllExist) {
                break;
            }
            if (inserted >= target) {
                break;
            }
            pageNo++;
        }

        FetchResult result = new FetchResult();
        result.setInsertedCount(inserted);
        result.setTotalCount((int) repository.count());
        DltNumber latest = repository.findRecent(org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        if (latest != null) {
            result.setLatestIssue(latest.getIssue());
            result.setLatestDate(latest.getDrawDate() == null ? "" : latest.getDrawDate().toString());
        }
        return result;
    }

    /**
     * 拉取某一页数据
     */
    private List<DltNumber> fetchPage(int pageNo) {
        try {
            String url = crawlUrl + "?gameNo=" + gameNo
                    + "&provinceId=0&pageSize=" + pageSize
                    + "&isVerify=1&pageNo=" + pageNo;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(readTimeout))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                    .header("Referer", "https://www.lottery.gov.cn/")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("爬取接口返回非200状态码: {}", response.statusCode());
                return null;
            }
            return parse(response.body());
        } catch (Exception e) {
            log.error("爬取大乐透数据失败 pageNo={}: {}", pageNo, e.getMessage());
            return null;
        }
    }

    /**
     * 解析接口 JSON 响应
     */
    private List<DltNumber> parse(String body) throws Exception {
        List<DltNumber> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(body);
        JsonNode value = root.path("value");
        JsonNode list = value.path("list");
        if (list == null || !list.isArray()) {
            return result;
        }
        for (JsonNode item : list) {
            String issue = item.path("lotteryDrawNum").asText();
            String drawResult = item.path("lotteryDrawResult").asText();
            String drawTime = item.path("lotteryDrawTime").asText();
            if (issue.isEmpty() || drawResult.isEmpty()) {
                continue;
            }
            int[] numbers = parseResult(drawResult);
            if (numbers == null) {
                continue;
            }
            LocalDate date = parseDate(drawTime);
            DltNumber dlt = new DltNumber(
                    issue,
                    new int[]{numbers[0], numbers[1], numbers[2], numbers[3], numbers[4]},
                    new int[]{numbers[5], numbers[6]},
                    date);
            result.add(dlt);
        }
        return result;
    }

    /**
     * 解析开奖结果 "03 10 12 20 25 01 09" -> [3,10,12,20,25,1,9]
     */
    private int[] parseResult(String drawResult) {
        String[] parts = drawResult.trim().split("\\s+");
        if (parts.length < 7) {
            return null;
        }
        int[] nums = new int[7];
        for (int i = 0; i < 7; i++) {
            try {
                nums[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // 前区、后区各自排序，确保升序
        Arrays.sort(nums, 0, 5);
        Arrays.sort(nums, 5, 7);
        return nums;
    }

    private LocalDate parseDate(String drawTime) {
        try {
            if (drawTime != null && !drawTime.isEmpty()) {
                return LocalDate.parse(drawTime.trim());
            }
        } catch (Exception e) {
            log.warn("解析开奖日期失败: {}", drawTime);
        }
        return LocalDate.now();
    }
}
