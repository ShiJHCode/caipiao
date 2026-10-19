package com.caipiao.dlt.service;

import com.caipiao.dlt.dto.BetNumber;
import com.caipiao.dlt.dto.RecommendResponse;
import com.caipiao.dlt.dto.StatisticsInfo;
import com.caipiao.dlt.entity.DltNumber;
import com.caipiao.dlt.repository.DltNumberRepository;
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
 * 选号推荐服务
 * <p>
 * 生成 5 注号码：
 * - 前 3 注：以"概率最大"的分布特征为核心（3奇2偶/2奇3偶、跨度20-30、三区覆盖），
 *   并依据各特征"回归期望"（当前遗漏 >= 理论回归周期）动态命中连号、重号等欠回归特征。
 * - 后 2 注：以"最冷号"策略为核心（最近冷号窗口内频次最低的号码），
 *   再叠加回归期望与概率最大约束。
 */
@Service
public class RecommendService {

    private static final int COLD_FRONT_POOL = 10;   // 前区冷号池大小
    private static final int COLD_BACK_POOL = 6;     // 后区冷号池大小
    private static final int MAX_ATTEMPT = 200000;

    private final StatisticsService statisticsService;
    private final DltNumberRepository repository;

    public RecommendService(StatisticsService statisticsService, DltNumberRepository repository) {
        this.statisticsService = statisticsService;
        this.repository = repository;
    }

    public RecommendResponse generate() {
        StatisticsInfo stats = statisticsService.computeStatistics();
        if (stats.getSampleSize() == 0) {
            throw new IllegalStateException("暂无开奖数据，请先获取最新开奖号码");
        }

        RecommendResponse resp = new RecommendResponse();
        resp.setStatistics(stats);
        resp.setDataWindow(stats.getSampleSize());

        // 冷号池
        List<Integer> coldFronts = stats.getFrontFreq().stream()
                .limit(COLD_FRONT_POOL)
                .map(StatisticsInfo.FreqItem::getNumber)
                .collect(Collectors.toList());
        List<Integer> coldBacks = stats.getBackFreq().stream()
                .limit(COLD_BACK_POOL)
                .map(StatisticsInfo.FreqItem::getNumber)
                .collect(Collectors.toList());
        resp.setColdFronts(coldFronts);
        resp.setColdBacks(coldBacks);

        // 回归期望状态
        boolean consecExpecting = isExpecting(stats.getConsecutiveStats(), "至少1组连号");
        boolean repeatExpecting = isExpecting(stats.getRepeatStats(), "至少1个重号");

        // 上一期号码（用于重号策略）
        int[] latestFronts = null;
        DltNumber latest = repository.findRecent(PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
        if (latest != null) {
            latestFronts = latest.fronts();
        }

        Random random = new Random();
        List<BetNumber> bets = new ArrayList<>();

        // ---------- 前 3 注：回归期望 + 概率最大 ----------
        // 注1：3奇2偶 + 跨度20-30 + 三区覆盖，动态叠加欠回归的连号/重号
        Integer forcedRepeat = null;
        String strategy1 = "回归期望策略：概率最大的 3奇2偶 + 跨度20-30 + 三区覆盖";
        if (repeatExpecting && latestFronts != null) {
            forcedRepeat = latestFronts[random.nextInt(latestFronts.length)];
            strategy1 += "，并带 1 个上期重号（重号欠回归）";
        }
        boolean withConsec1 = consecExpecting;
        if (withConsec1) {
            strategy1 += "，并带 1 组连号（连号欠回归）";
        }
        int[] f1 = generateFront(random, null, 3, withConsec1, true, true, forcedRepeat);
        bets.add(buildBet(1, strategy1, f1, generateBack(random, null, 1)));

        // 注2：3奇2偶 + 跨度20-30 + 三区覆盖（无连号/重号）
        int[] f2 = generateFront(random, null, 3, false, true, true, null);
        bets.add(buildBet(2, "回归期望策略：概率最大的 3奇2偶 + 跨度20-30 + 三区覆盖", f2,
                generateBack(random, null, 1)));

        // 注3：2奇3偶 + 跨度20-30 + 三区覆盖
        int[] f3 = generateFront(random, null, 2, false, true, true, null);
        bets.add(buildBet(3, "回归期望策略：次优 2奇3偶 + 跨度20-30 + 三区覆盖", f3,
                generateBack(random, null, 1)));

        // ---------- 后 2 注：最冷号 + 回归期望 + 概率最大 ----------
        int[] f4 = generateColdFront(random, coldFronts, 3);
        bets.add(buildBet(4, "最冷号策略：优先冷号池(" + abbreviate(coldFronts) + ")，叠加 3奇2偶 + 跨度20-30 + 三区覆盖", f4,
                generateBack(random, coldBacks, 1)));

        int[] f5 = generateColdFront(random, coldFronts, 2);
        bets.add(buildBet(5, "最冷号策略：优先冷号池(" + abbreviate(coldFronts) + ")，叠加 2奇3偶 + 跨度20-30 + 三区覆盖", f5,
                generateBack(random, coldBacks, -1)));

        resp.setBets(bets);
        return resp;
    }

    // ---------------- 前区生成 ----------------

    /**
     * 通用前区生成：随机采样 + 多重约束校验，逐级降级
     *
     * @param forcedNumber 强制包含的号码（重号策略），可为 null
     */
    private int[] generateFront(Random r, List<Integer> pool, int targetOdd,
                                boolean requireConsecutive, boolean requireThreeZone,
                                boolean requireSpan20to30, Integer forcedNumber) {
        int[] res = tryGenerate(r, pool, targetOdd, requireConsecutive, requireThreeZone, requireSpan20to30, forcedNumber);
        if (res != null) return res;
        res = tryGenerate(r, pool, targetOdd, requireConsecutive, requireThreeZone, false, forcedNumber);
        if (res != null) return res;
        res = tryGenerate(r, pool, targetOdd, requireConsecutive, false, false, forcedNumber);
        if (res != null) return res;
        res = tryGenerate(r, pool, targetOdd, false, false, false, forcedNumber);
        if (res != null) return res;
        return fallbackFront(r, pool, targetOdd, forcedNumber);
    }

    /**
     * 冷号前区生成：优先冷号池，受限时逐级放宽并最终用全池兜底
     */
    private int[] generateColdFront(Random r, List<Integer> coldPool, int targetOdd) {
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
        return generateFront(r, null, targetOdd, false, true, true, null);
    }

    private int[] tryGenerate(Random r, List<Integer> pool, int targetOdd,
                              boolean requireConsecutive, boolean requireThreeZone,
                              boolean requireSpan20to30, Integer forcedNumber) {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            int[] nums = sampleFront(r, pool, forcedNumber);
            if (nums != null && checkFront(nums, targetOdd, requireConsecutive, requireThreeZone, requireSpan20to30)) {
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
            while (s.size() < 5) {
                s.add(1 + r.nextInt(35));
            }
            int[] nums = s.stream().mapToInt(Integer::intValue).toArray();
            if (checkFront(nums, targetOdd, false, strict, strict)) {
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

    /**
     * 采样 5 个不重复号码（升序）。forcedNumber 非空则强制包含。
     */
    private int[] sampleFront(Random r, List<Integer> pool, Integer forcedNumber) {
        Set<Integer> s = new TreeSet<>();
        if (forcedNumber != null) {
            s.add(forcedNumber);
        }
        while (s.size() < 5) {
            int num = (pool == null || pool.isEmpty())
                    ? 1 + r.nextInt(35)
                    : pool.get(r.nextInt(pool.size()));
            s.add(num);
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean checkFront(int[] nums, int targetOdd, boolean requireConsecutive,
                               boolean requireThreeZone, boolean requireSpan20to30) {
        if (StatisticsService.countOdd(nums) != targetOdd) return false;
        if (requireThreeZone && !coversThreeZone(nums)) return false;
        if (requireSpan20to30) {
            int span = nums[4] - nums[0];
            if (span < 20 || span > 30) return false;
        }
        if (requireConsecutive && StatisticsService.maxConsecutiveRun(nums) < 2) return false;
        return true;
    }

    private int[] fallbackFront(Random r, List<Integer> pool, int targetOdd, Integer forcedNumber) {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            int[] nums = sampleFront(r, null, forcedNumber);
            if (nums != null && StatisticsService.countOdd(nums) == targetOdd) return nums;
        }
        return sampleFront(r, null, forcedNumber);
    }

    private boolean coversThreeZone(int[] nums) {
        boolean[] zone = new boolean[3];
        for (int n : nums) zone[StatisticsService.zoneOf(n)] = true;
        return zone[0] && zone[1] && zone[2];
    }

    // ---------------- 后区生成 ----------------

    /**
     * @param targetOdd 期望奇数个数（0/1/2），-1 表示不限制
     */
    private int[] generateBack(Random r, List<Integer> pool, int targetOdd) {
        for (int i = 0; i < MAX_ATTEMPT; i++) {
            Set<Integer> s = new TreeSet<>();
            while (s.size() < 2) {
                int num = (pool == null || pool.isEmpty())
                        ? 1 + r.nextInt(12)
                        : pool.get(r.nextInt(pool.size()));
                s.add(num);
            }
            int[] nums = s.stream().mapToInt(Integer::intValue).toArray();
            if (targetOdd < 0 || StatisticsService.countOdd(nums) == targetOdd) {
                return nums;
            }
        }
        Set<Integer> s = new TreeSet<>();
        while (s.size() < 2) {
            s.add(1 + r.nextInt(12));
        }
        return s.stream().mapToInt(Integer::intValue).toArray();
    }

    // ---------------- 组装 ----------------

    private BetNumber buildBet(int index, String strategy, int[] fronts, int[] backs) {
        BetNumber bet = new BetNumber();
        bet.setIndex(index);
        bet.setStrategy(strategy);
        bet.setFronts(Arrays.stream(fronts).boxed().collect(Collectors.toList()));
        bet.setBacks(Arrays.stream(backs).boxed().collect(Collectors.toList()));

        int odd = StatisticsService.countOdd(fronts);
        bet.setOddEven(odd + "奇" + (5 - odd) + "偶");
        bet.setSpan(fronts[4] - fronts[0]);
        bet.setZoneDesc(zoneDesc(fronts));

        boolean hasConsec = StatisticsService.maxConsecutiveRun(fronts) >= 2;
        bet.setHasConsecutive(hasConsec);

        List<String> tags = new ArrayList<>();
        tags.add(bet.getOddEven());
        tags.add(bet.getSpan() >= 20 && bet.getSpan() <= 30 ? "跨度20-30" : "跨度" + bet.getSpan());
        tags.add(coversThreeZone(fronts) ? "三区覆盖" : "未覆盖三区");
        if (hasConsec) tags.add("含连号");
        bet.setTags(tags);

        return bet;
    }

    private String zoneDesc(int[] fronts) {
        int[] zone = new int[3];
        for (int f : fronts) zone[StatisticsService.zoneOf(f)]++;
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
