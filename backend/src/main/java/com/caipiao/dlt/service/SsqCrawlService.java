package com.caipiao.dlt.service;

import com.caipiao.dlt.dto.FetchResult;
import com.caipiao.dlt.entity.SsqNumber;
import com.caipiao.dlt.repository.SsqNumberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
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
 * 双色球历史开奖数据爬取服务
 * 数据源：中国福利彩票官网接口（name=ssq）
 */
@Service
public class SsqCrawlService {

    private static final Logger log = LoggerFactory.getLogger(SsqCrawlService.class);

    private final SsqNumberRepository repository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${lottery.ssq-crawl.url}")
    private String crawlUrl;

    @Value("${lottery.ssq-crawl.default-count:150}")
    private int defaultCount;

    @Value("${lottery.crawl.read-timeout:15000}")
    private int readTimeout;

    public SsqCrawlService(SsqNumberRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(10000))
                .build();
    }

    /**
     * 增量拉取最新开奖数据（默认 150 期，接口一次返回，按期号去重入库）
     */
    public FetchResult fetchLatest(int count) {
        int target = count > 0 ? count : defaultCount;
        int inserted = 0;

        List<SsqNumber> list = fetch(target);
        if (list != null) {
            for (SsqNumber item : list) {
                if (repository.existsByIssue(item.getIssue())) {
                    continue;
                }
                repository.save(item);
                inserted++;
            }
        }

        FetchResult result = new FetchResult();
        result.setInsertedCount(inserted);
        result.setTotalCount((int) repository.count());
        SsqNumber latest = repository.findRecent(PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        if (latest != null) {
            result.setLatestIssue(latest.getIssue());
            result.setLatestDate(latest.getDrawDate() == null ? "" : latest.getDrawDate().toString());
        }
        return result;
    }

    private List<SsqNumber> fetch(int count) {
        try {
            String url = crawlUrl + "?name=ssq&issueCount=" + count;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(readTimeout))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
                    .header("Referer", "https://www.cwl.gov.cn/")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                log.warn("双色球接口返回非200状态码: {}", response.statusCode());
                return null;
            }
            return parse(response.body());
        } catch (Exception e) {
            log.error("爬取双色球数据失败: {}", e.getMessage());
            return null;
        }
    }

    private List<SsqNumber> parse(String body) throws Exception {
        List<SsqNumber> result = new ArrayList<>();
        JsonNode root = objectMapper.readTree(body);
        JsonNode list = root.path("result");
        if (list == null || !list.isArray()) {
            return result;
        }
        for (JsonNode item : list) {
            String issue = item.path("code").asText();
            String red = item.path("red").asText();
            String blue = item.path("blue").asText();
            String date = item.path("date").asText();
            if (issue.isEmpty() || red.isEmpty() || blue.isEmpty()) {
                continue;
            }
            int[] reds = parseReds(red);
            int blueNum;
            try {
                blueNum = Integer.parseInt(blue.trim());
            } catch (NumberFormatException e) {
                continue;
            }
            if (reds == null) {
                continue;
            }
            LocalDate drawDate = parseDate(date);
            result.add(new SsqNumber(issue, reds, blueNum, drawDate));
        }
        return result;
    }

    /**
     * 解析红球 "01,12,14,18,30,31" -> 升序 int[6]
     */
    private int[] parseReds(String red) {
        String[] parts = red.split(",");
        if (parts.length != 6) {
            return null;
        }
        int[] nums = new int[6];
        for (int i = 0; i < 6; i++) {
            try {
                nums[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        Arrays.sort(nums);
        return nums;
    }

    private LocalDate parseDate(String date) {
        try {
            if (date != null && date.length() >= 10) {
                return LocalDate.parse(date.substring(0, 10));
            }
        } catch (Exception e) {
            log.warn("解析双色球开奖日期失败: {}", date);
        }
        return LocalDate.now();
    }
}
