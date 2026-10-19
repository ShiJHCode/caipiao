package com.caipiao.dlt.service;

import com.caipiao.dlt.dto.StatisticsInfo;
import com.caipiao.dlt.entity.DltNumber;
import com.caipiao.dlt.repository.DltNumberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计分析服务
 * 依据大乐透历史开奖数据计算各维度分布，并与理论值对比
 */
@Service
public class StatisticsService {

    private final DltNumberRepository repository;

    @Value("${lottery.crawl.cold-window:30}")
    private int coldWindow;

    public StatisticsService(DltNumberRepository repository) {
        this.repository = repository;
    }

    /**
     * 计算全量统计信息
     * 分布统计使用全部历史数据；冷号频次使用最近 coldWindow 期
     */
    public StatisticsInfo computeStatistics() {
        List<DltNumber> all = repository.findAll(Sort.by(Sort.Direction.ASC, "drawDate", "id"));
        StatisticsInfo info = new StatisticsInfo();
        if (all.isEmpty()) {
            info.setSampleSize(0);
            return info;
        }
        info.setSampleSize(all.size());
        fillDistributionStats(info, all);
        fillOmission(info, all);
        fillFrequency(info, all);
        return info;
    }

    // ---------------- 各维度分布统计 ----------------

    private void fillDistributionStats(StatisticsInfo info, List<DltNumber> all) {
        int n = all.size();

        // 1. 空间分布（前区三区：1-12 / 13-24 / 25-35）
        int allThree = 0, oneCut = 0, twoCut = 0;
        // 2. 前区奇偶
        int[] oddEven = new int[6]; // index = 奇数个数
        // 3. 跨度
        int spanLt15 = 0, span15to19 = 0, span20to30 = 0, spanGt30 = 0;
        // 4. 连号
        int consecNone = 0, consec1 = 0, consec2 = 0, consec3 = 0, consec4 = 0, consec5 = 0;
        // 5. 后区奇偶
        int back1odd = 0, back2odd = 0, back0odd = 0;
        // 6. 重号（从第2期起）
        int repeat0 = 0, repeat1 = 0, repeat2 = 0, repeat3 = 0, repeatGt3 = 0;

        for (int i = 0; i < n; i++) {
            DltNumber cur = all.get(i);
            int[] fronts = cur.fronts();

            // 空间分布
            int[] zone = new int[3];
            for (int f : fronts) {
                zone[zoneOf(f)]++;
            }
            int emptyZones = 0;
            for (int z : zone) {
                if (z == 0) emptyZones++;
            }
            if (emptyZones == 0) {
                allThree++;
            } else if (emptyZones == 1) {
                oneCut++;
            } else {
                twoCut++;
            }

            // 奇偶
            int odd = countOdd(fronts);
            oddEven[odd]++;

            // 跨度
            int span = fronts[4] - fronts[0];
            if (span < 15) spanLt15++;
            else if (span <= 19) span15to19++;
            else if (span <= 30) span20to30++;
            else spanGt30++;

            // 连号
            int maxRun = maxConsecutiveRun(fronts);
            int groupCount = consecutiveGroupCount(fronts);
            if (maxRun <= 1) {
                consecNone++;
            } else if (maxRun == 5) {
                consec5++;
            } else if (maxRun == 4) {
                consec4++;
            } else if (maxRun == 3) {
                consec3++;
            } else { // maxRun == 2
                if (groupCount >= 2) consec2++;
                else consec1++;
            }

            // 后区奇偶
            int backOdd = countOdd(cur.backs());
            if (backOdd == 1) back1odd++;
            else if (backOdd == 2) back2odd++;
            else back0odd++;

            // 重号（与上一期前区交集）
            if (i > 0) {
                int[] prev = all.get(i - 1).fronts();
                int repeat = intersectCount(fronts, prev);
                if (repeat == 0) repeat0++;
                else if (repeat == 1) repeat1++;
                else if (repeat == 2) repeat2++;
                else if (repeat == 3) repeat3++;
                else repeatGt3++;
            }
        }

        int repeatN = Math.max(0, n - 1);

        // 空间分布统计项
        info.getZoneStats().add(statItem("三区都有号码", allThree, n, 66.81, 1.5));
        info.getZoneStats().add(statItem("至少断一区", oneCut + twoCut, n, 33.19, 3.01));
        info.getZoneStats().add(statItem("断两区(全落一区)", twoCut, n, 0.63, 158.7));

        // 奇偶统计项
        info.getOddEvenStats().add(statItem("3奇2偶", oddEven[3], n, 34.19, 2.92));
        info.getOddEvenStats().add(statItem("2奇3偶", oddEven[2], n, 32.05, 3.12));
        info.getOddEvenStats().add(statItem("4奇1偶", oddEven[4], n, 16.02, 6.24));
        info.getOddEvenStats().add(statItem("1奇4偶", oddEven[1], n, 14.46, 6.92));
        info.getOddEvenStats().add(statItem("5奇0偶", oddEven[5], n, 2.64, 37.89));
        info.getOddEvenStats().add(statItem("0奇5偶", oddEven[0], n, 2.07, 48.31));

        // 跨度统计项
        info.getSpanStats().add(statItem("跨度20-30", span20to30, n, 63.42, 1.58));
        info.getSpanStats().add(statItem("跨度15-19", span15to19, n, 20.95, 4.77));
        info.getSpanStats().add(statItem("跨度<15", spanLt15, n, 7.09, 14.1));
        info.getSpanStats().add(statItem("跨度>30", spanGt30, n, 8.54, 11.71));

        // 重号统计项
        info.getRepeatStats().add(statItem("至少1个重号", repeat1 + repeat2 + repeat3 + repeatGt3, repeatN, 56.1, 1.78));
        info.getRepeatStats().add(statItem("无重号", repeat0, repeatN, 43.9, 2.28));
        info.getRepeatStats().add(statItem("1个重号", repeat1, repeatN, 41.69, 2.40));
        info.getRepeatStats().add(statItem("2个重号", repeat2, repeatN, 12.51, 8));
        info.getRepeatStats().add(statItem("3个重号", repeat3, repeatN, 1.61, 62.1));

        // 连号统计项
        info.getConsecutiveStats().add(statItem("至少1组连号", consec1 + consec2 + consec3 + consec4 + consec5, n, 47.66, 2.1));
        info.getConsecutiveStats().add(statItem("1组连号", consec1, n, 38.5, 2.6));
        info.getConsecutiveStats().add(statItem("2组连号", consec2, n, 8.1, 12.3));
        info.getConsecutiveStats().add(statItem("3连号", consec3, n, 1.7, 58));
        info.getConsecutiveStats().add(statItem("4连号", consec4, n, 0.08, 1200));
        info.getConsecutiveStats().add(statItem("5连号", consec5, n, 0.0095, 10472));

        // 后区奇偶统计项
        info.getBackOddEvenStats().add(statItem("1奇1偶", back1odd, n, 54.55, 1.83));
        info.getBackOddEvenStats().add(statItem("2奇0偶", back2odd, n, 22.73, 4.4));
        info.getBackOddEvenStats().add(statItem("0奇2偶", back0odd, n, 22.73, 4.4));
    }

    /**
     * 计算各特征当前遗漏期数，并标记"欠回归"状态
     */
    private void fillOmission(StatisticsInfo info, List<DltNumber> allAsc) {
        List<DltNumber> desc = new ArrayList<>(allAsc);
        java.util.Collections.reverse(desc); // 最新在前

        fillOmitForGroup(info.getZoneStats(), desc);
        fillOmitForGroup(info.getOddEvenStats(), desc);
        fillOmitForGroup(info.getSpanStats(), desc);
        fillOmitForGroup(info.getRepeatStats(), desc);
        fillOmitForGroup(info.getConsecutiveStats(), desc);
        fillOmitForGroup(info.getBackOddEvenStats(), desc);
    }

    private void fillOmitForGroup(List<StatisticsInfo.StatItem> items, List<DltNumber> desc) {
        for (StatisticsInfo.StatItem item : items) {
            int omit = computeFeatureOmit(item.getName(), desc);
            item.setOmit(omit);
            item.setExpecting(omit >= item.getTheoryPeriod());
        }
    }

    /**
     * 计算某特征距上次出现已遗漏的期数（0=最新一期刚命中）
     */
    private int computeFeatureOmit(String name, List<DltNumber> desc) {
        for (int i = 0; i < desc.size(); i++) {
            DltNumber cur = desc.get(i);
            DltNumber prev = (i + 1 < desc.size()) ? desc.get(i + 1) : null; // 更早一期
            if (matchFeature(name, cur, prev)) {
                return i;
            }
        }
        return desc.size();
    }

    private boolean matchFeature(String name, DltNumber cur, DltNumber prev) {
        int[] fronts = cur.fronts();
        int odd = countOdd(fronts);
        int span = fronts[4] - fronts[0];
        int maxRun = maxConsecutiveRun(fronts);
        int group = consecutiveGroupCount(fronts);
        int backOdd = countOdd(cur.backs());

        switch (name) {
            // 空间分布
            case "三区都有号码":
                return coversThreeZone(fronts);
            case "至少断一区":
                return !coversThreeZone(fronts);
            case "断两区(全落一区)":
                return zoneOf(fronts[0]) == zoneOf(fronts[4])
                        && (fronts[4] - fronts[0]) <= 11
                        && countNonEmptyZones(fronts) == 1;
            // 前区奇偶
            case "3奇2偶": return odd == 3;
            case "2奇3偶": return odd == 2;
            case "4奇1偶": return odd == 4;
            case "1奇4偶": return odd == 1;
            case "5奇0偶": return odd == 5;
            case "0奇5偶": return odd == 0;
            // 跨度
            case "跨度20-30": return span >= 20 && span <= 30;
            case "跨度15-19": return span >= 15 && span <= 19;
            case "跨度<15": return span < 15;
            case "跨度>30": return span > 30;
            // 重号
            case "至少1个重号": {
                if (prev == null) return false;
                return intersectCount(fronts, prev.fronts()) >= 1;
            }
            case "无重号": {
                if (prev == null) return false;
                return intersectCount(fronts, prev.fronts()) == 0;
            }
            case "1个重号": {
                if (prev == null) return false;
                return intersectCount(fronts, prev.fronts()) == 1;
            }
            case "2个重号": {
                if (prev == null) return false;
                return intersectCount(fronts, prev.fronts()) == 2;
            }
            case "3个重号": {
                if (prev == null) return false;
                return intersectCount(fronts, prev.fronts()) == 3;
            }
            // 连号
            case "至少1组连号": return maxRun >= 2;
            case "1组连号": return maxRun == 2 && group == 1;
            case "2组连号": return maxRun == 2 && group >= 2;
            case "3连号": return maxRun == 3;
            case "4连号": return maxRun == 4;
            case "5连号": return maxRun == 5;
            // 后区奇偶
            case "1奇1偶": return backOdd == 1;
            case "2奇0偶": return backOdd == 2;
            case "0奇2偶": return backOdd == 0;
            default:
                return false;
        }
    }

    private boolean coversThreeZone(int[] fronts) {
        boolean[] zone = new boolean[3];
        for (int f : fronts) zone[zoneOf(f)] = true;
        return zone[0] && zone[1] && zone[2];
    }

    private int countNonEmptyZones(int[] fronts) {
        boolean[] zone = new boolean[3];
        for (int f : fronts) zone[zoneOf(f)] = true;
        int c = 0;
        for (boolean b : zone) if (b) c++;
        return c;
    }

    /**
     * 计算号码频次与遗漏（最近 coldWindow 期）
     */
    private void fillFrequency(StatisticsInfo info, List<DltNumber> all) {
        int window = Math.min(coldWindow, all.size());
        List<DltNumber> recent = all.subList(all.size() - window, all.size());

        // 前区频次
        Map<Integer, Integer> frontFreq = new HashMap<>();
        Map<Integer, Integer> backFreq = new HashMap<>();
        for (DltNumber d : recent) {
            for (int f : d.fronts()) {
                frontFreq.merge(f, 1, Integer::sum);
            }
            for (int b : d.backs()) {
                backFreq.merge(b, 1, Integer::sum);
            }
        }

        // 前区 1-35
        for (int num = 1; num <= 35; num++) {
            int freq = frontFreq.getOrDefault(num, 0);
            int omit = computeOmit(recent, num, true);
            info.getFrontFreq().add(new StatisticsInfo.FreqItem(num, freq, omit));
        }
        // 后区 1-12
        for (int num = 1; num <= 12; num++) {
            int freq = backFreq.getOrDefault(num, 0);
            int omit = computeOmit(recent, num, false);
            info.getBackFreq().add(new StatisticsInfo.FreqItem(num, freq, omit));
        }

        // 按频次升序（冷号在前）、同频次按遗漏降序
        info.getFrontFreq().sort((a, b) -> {
            if (a.getFreq() != b.getFreq()) return Integer.compare(a.getFreq(), b.getFreq());
            return Integer.compare(b.getOmit(), a.getOmit());
        });
        info.getBackFreq().sort((a, b) -> {
            if (a.getFreq() != b.getFreq()) return Integer.compare(a.getFreq(), b.getFreq());
            return Integer.compare(b.getOmit(), a.getOmit());
        });
    }

    private int computeOmit(List<DltNumber> recent, int num, boolean front) {
        for (int i = recent.size() - 1; i >= 0; i--) {
            int[] arr = front ? recent.get(i).fronts() : recent.get(i).backs();
            if (contains(arr, num)) {
                return recent.size() - 1 - i;
            }
        }
        return recent.size();
    }

    // ---------------- 工具方法 ----------------

    public static int zoneOf(int num) {
        if (num <= 12) return 0;
        if (num <= 24) return 1;
        return 2;
    }

    public static int countOdd(int[] nums) {
        int c = 0;
        for (int n : nums) {
            if ((n & 1) == 1) c++;
        }
        return c;
    }

    public static boolean contains(int[] arr, int v) {
        for (int x : arr) {
            if (x == v) return true;
        }
        return false;
    }

    public static int intersectCount(int[] a, int[] b) {
        int c = 0;
        for (int x : a) {
            for (int y : b) {
                if (x == y) {
                    c++;
                    break;
                }
            }
        }
        return c;
    }

    /**
     * 最长连续段长度（排序后的数组）
     */
    public static int maxConsecutiveRun(int[] sorted) {
        int max = 1, cur = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] == sorted[i - 1] + 1) {
                cur++;
                max = Math.max(max, cur);
            } else {
                cur = 1;
            }
        }
        return max;
    }

    /**
     * 连号组数（长度>=2 的连续段个数）
     */
    public static int consecutiveGroupCount(int[] sorted) {
        int groups = 0, cur = 1;
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] == sorted[i - 1] + 1) {
                cur++;
            } else {
                if (cur >= 2) groups++;
                cur = 1;
            }
        }
        if (cur >= 2) groups++;
        return groups;
    }

    private StatisticsInfo.StatItem statItem(String name, int count, int sample, double theoryRatio, double theoryPeriod) {
        StatisticsInfo.StatItem item = new StatisticsInfo.StatItem();
        item.setName(name);
        item.setCount(count);
        item.setActualRatio(sample > 0 ? round4(100.0 * count / sample) : 0);
        item.setTheoryRatio(theoryRatio);
        item.setTheoryPeriod(theoryPeriod);
        return item;
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
