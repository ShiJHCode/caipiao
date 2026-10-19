package com.caipiao.dlt.service;

import com.caipiao.dlt.dto.BetNumber;
import com.caipiao.dlt.dto.RecommendResponse;
import com.caipiao.dlt.dto.StatisticsInfo;
import com.caipiao.dlt.entity.SsqNumber;
import com.caipiao.dlt.repository.SsqNumberRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 双色球选号推荐服务（逻辑参考大乐透）
 * <p>
 * 红球 1-33 选 6，蓝球 1-16 选 1。
 * 前 3 注：概率最大的分布特征（3奇3偶/4奇2偶、跨度>=20、三区覆盖）+ 回归期望动态命中连号/重号。
 * 后 2 注：最冷号策略 + 回归期望 + 概率最大。
 */
@Service
public class SsqRecommendService {

    private static final int COLD_RED_POOL = 10;    // 红球冷号池大小
    private static final int COLD_BLUE_POOL = 6;    // 蓝球冷号池大小
    private static final int MAX_ATTEMPT = 200000;

    private final SsqStatisticsService statisticsService;
    private final SsqNumberRepository repository;

    public SsqRecommendService(SsqStatisticsService statisticsService, SsqNumberRepository repository) {
        this.statisticsService = statisticsService;
        this.repository = repository;
    }

    public RecommendResponse generate() {
        StatisticsInfo stats = statisticsService.computeStatistics();
        if (stats.getSampleSize() == 0) {
            throw new IllegalStateException("暂无开奖数据，请先获取最新双色球开奖号码");
        }

        RecommendResponse resp = new RecommendResponse();
        resp.setStatistics(stats);
        resp.setDataWindow(stats.getSampleSize());

        List<Integer> coldReds = stats.getFrontFreq().stream()
                .limit(COLD_RED_POOL)
                .map(StatisticsInfo.FreqItem::getNumber)
                .collect(Collectors.toList());
        List<Integer> coldBlues = stats.getBackFreq().stream()
                .limit(COLD_BLUE_POOL)
                .map(StatisticsInfo.FreqItem::getNumber)
                .collect(Collectors.toList());
        resp.setColdFronts(coldReds);
        resp.setColdBacks(coldBlues);

        boolean consecExpecting = isExpecting(stats.getConsecutiveStats(), "至少1组连号");
        boolean repeatExpecting = isExpecting(stats.getRepeatStats(), "至少1个重号");

        int[] latestReds = null;
        SsqNumber latest = repository.findRecent(PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        if (latest != null) {
            latestReds = latest.reds();
        }

        Random random = new Random();
        List<BetNumber> bets = new ArrayList<>();

        // ---------- 前 3 注：回归期望 + 概率最大 ----------
        Integer forcedRepeat = null;
        String strategy1 = "回归期望策略：概率最大的 3奇3偶 + 跨度>=20 + 三区覆盖";
        if (repeatExpecting && latestReds != null) {
            forcedRepeat = latestReds[random.nextInt(latestReds.length)];
            strategy1 += "，并带 1 个上期重号（重号欠回归）";
        }
        boolean withConsec1 = consecExpecting;
        if (withConsec1) {
            strategy1 += "，并带 1 组连号（连号欠回归）";
        }
        int[] r1 = generateRed(random, null, 3, withConsec1, true, true, forcedRepeat);
        bets.add(buildBet(1, strategy1, r1, generateBlue(random, null)));

        int[] r2 = generateRed(random, null, 3, false, true, true, null);
        bets.add(buildBet(2, "回归期望策略：概率最大的 3奇3偶 + 跨度>=20 + 三区覆盖", r2,
                generateBlue(random, null)));

        int[] r3 = generateRed(random, null, 4, false, true, true, null);
        bets.add(buildBet(3, "回归期望策略：次优 4奇2偶 + 跨度>=20 + 三区覆盖", r3,
                generateBlue(random, null)));

        // ---------- 后 2 注：最冷号 + 回归期望 + 概率最大 ----------
        int[] r4 = generateColdRed(random, coldReds, 3);
        bets.add(buildBet(4, "最冷号策略：优先红球冷号池(" + abbreviate(coldReds) + ")，叠加 3奇3偶 + 跨度>=20 + 三区覆盖", r4,
                generateBlue(random, coldBlues)));

        int[] r5 = generateColdRed(random, coldReds, 2);
        bets.add(buildBet(5, "最冷号策略：优先红球冷号池(" + abbreviate(coldReds) + ")，叠加 2奇4偶 + 跨度>=20 + 三区覆盖", r5,
                generateBlue(random, coldBlues)));

        resp.setBets(bets);
        return resp;
    }

    // ---------------- 红球生成 ----------------

    private int[] generateRed(Random r, List<Integer> pool, int targetOdd,
                              boolean requireConsecutive, boolean requireThreeZone,
                              boolean requireSpan, Integer forcedNumber) {
        int[] res = tryGenerate(r, pool, targetOdd, requireConsecutive, requireThreeZone, requireSpan, forcedNumber);
        if (res != null) return res;
        res = tryGenerate(r, pool, targetOdd, requireConsecutive, requireThreeZone, false, forcedNumber);
        if (res != null) return res;
        res = tryGenerate(r, pool, targetOdd, requireConsecutive, false, false, forcedNumber);
        if (res != null) return res;
        res = tryGenerate(r, pool, targetOdd, false, false, false, forcedNumber);
        if (res != null) return res;
        return fallbackRed(r, pool, targetOdd, forcedNumber);
    }

    private int[] generateColdRed(Random r, List<Integer> coldPool, int targetOdd) {
        int[] res = tryGenerate(r, coldPool, targetOdd, false, true, true, null);
        if (res != null) return res;
        res = tryGenerate(r, coldPool, targetOdd, false, true, false, null);
        if (res != null) return res;
        res = tryGenerate(r, coldPool, targetOdd, false, false, false, null);
        if (res != null) return res;
        res = tryGenerateMixed(r, coldPool, targetOdd, 3, true);
        if (res != null) return res;
        res = tryGenerateMixed(r, coldPool, targetOdd, 2, false);
        if (res != null) return res;
        return generateRed(r, null, targetOdd, false, true, true, null);
    }

    private int[] tryGenerate(Random r, List<Integer> pool, int targetOdd,
                              boolean requireConsecutive, boolean requireThreeZone,
                              boolean requireSpan, Integer forcedNumber) {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            int[] nums = sampleRed(r, pool, forcedNumber);
            if (nums != null && checkRed(nums, targetOdd, requireConsecutive, requireThreeZone, requireSpan)) {
                return nums;
            }
        }
        return null;
    }

    private int[] tryGenerateMixed(Random r, List<Integer> coldPool, int targetOdd, int coldCount, boolean strict) {
        if (coldPool == null || coldPool.isEmpty()) {
            return null;
        }
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            Set<Integer> s = new TreeSet<>();
            while (countCold(s, coldPool) < coldCount) {
                s.add(coldPool.get(r.nextInt(coldPool.size())));
            }
            while (s.size() < 6) {
                s.add(1 + r.nextInt(33));
            }
            int[] nums = s.stream().mapToInt(Integer::intValue).toArray();
            if (checkRed(nums, targetOdd, false, strict, strict)) {
                return nums;
            }
        }
        return null;
    }

    private int countCold(Set<Integer> s, List<Integer> coldPool) {
        int c = 0;
        for (int n : s) {
            if (coldPool.contains(n)) c++;
        }
        return c;
    }

    private int[] sampleRed(Random r, List<Integer> pool, Integer forcedNumber) {
        Set<Integer> s = new TreeSet<>();
        if (forcedNumber != null) {
            s.add(forcedNumber);
        }
        while (s.size() < 6) {
            int num = (pool == null || pool.isEmpty())
                    ? 1 + r.nextInt(33)
                    : pool.get(r.nextInt(pool.size()));
            s.add(num);
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean checkRed(int[] nums, int targetOdd, boolean requireConsecutive,
                             boolean requireThreeZone, boolean requireSpan) {
        if (StatisticsService.countOdd(nums) != targetOdd) return false;
        if (requireThreeZone && !coversThreeZone(nums)) return false;
        if (requireSpan) {
            int span = nums[5] - nums[0];
            if (span < 20) return false;
        }
        if (requireConsecutive && StatisticsService.maxConsecutiveRun(nums) < 2) return false;
        return true;
    }

    private int[] fallbackRed(Random r, List<Integer> pool, int targetOdd, Integer forcedNumber) {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            int[] nums = sampleRed(r, null, forcedNumber);
            if (nums != null && StatisticsService.countOdd(nums) == targetOdd) return nums;
        }
        return sampleRed(r, null, forcedNumber);
    }

    private boolean coversThreeZone(int[] nums) {
        boolean[] zone = new boolean[3];
        for (int n : nums) zone[SsqStatisticsService.zoneOf(n)] = true;
        return zone[0] && zone[1] && zone[2];
    }

    // ---------------- 蓝球生成 ----------------

    private int generateBlue(Random r, List<Integer> pool) {
        if (pool == null || pool.isEmpty()) {
            return 1 + r.nextInt(16);
        }
        return pool.get(r.nextInt(pool.size()));
    }

    // ---------------- 组装 ----------------

    private BetNumber buildBet(int index, String strategy, int[] reds, int blue) {
        BetNumber bet = new BetNumber();
        bet.setIndex(index);
        bet.setStrategy(strategy);
        bet.setFronts(Arrays.stream(reds).boxed().collect(Collectors.toList()));
        bet.setBacks(List.of(blue));

        int odd = StatisticsService.countOdd(reds);
        bet.setOddEven(odd + "奇" + (6 - odd) + "偶");
        bet.setSpan(reds[5] - reds[0]);
        bet.setZoneDesc(zoneDesc(reds));

        boolean hasConsec = StatisticsService.maxConsecutiveRun(reds) >= 2;
        bet.setHasConsecutive(hasConsec);

        List<String> tags = new ArrayList<>();
        tags.add(bet.getOddEven());
        tags.add(bet.getSpan() >= 20 ? "跨度>=20" : "跨度" + bet.getSpan());
        tags.add(coversThreeZone(reds) ? "三区覆盖" : "未覆盖三区");
        if (hasConsec) tags.add("含连号");
        bet.setTags(tags);

        return bet;
    }

    private String zoneDesc(int[] reds) {
        int[] zone = new int[3];
        for (int r : reds) zone[SsqStatisticsService.zoneOf(r)]++;
        return "1区" + zone[0] + "个 / 2区" + zone[1] + "个 / 3区" + zone[2] + "个";
    }

    private boolean isExpecting(List<StatisticsInfo.StatItem> items, String name) {
        for (StatisticsInfo.StatItem item : items) {
            if (item.getName().equals(name)) {
                return item.isExpecting();
            }
        }
        return false;
    }

    private String abbreviate(List<Integer> nums) {
        return nums.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
