package com.caipiao.dlt.service;

import com.caipiao.dlt.dto.StatisticsInfo;
import com.caipiao.dlt.entity.SsqNumber;
import com.caipiao.dlt.repository.SsqNumberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 双色球统计分析服务
 * 红球 1-33 选 6，蓝球 1-16 选 1。
 * 理论概率由组合数学 C(33,6)=1107568 精确枚举 / 超几何分布计算得出。
 */
@Service
public class SsqStatisticsService {

    private final SsqNumberRepository repository;

    @Value("${lottery.crawl.cold-window:30}")
    private int coldWindow;

    public SsqStatisticsService(SsqNumberRepository repository) {
        this.repository = repository;
    }

    /**
     * 双色球红球三区：1-11 / 12-22 / 23-33
     */
    public static int zoneOf(int num) {
        if (num <= 11) return 0;
        if (num <= 22) return 1;
        return 2;
    }

    public StatisticsInfo computeStatistics() {
        List<SsqNumber> all = repository.findAll(Sort.by(Sort.Direction.ASC, "drawDate", "id"));
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

    private void fillDistributionStats(StatisticsInfo info, List<SsqNumber> all) {
        int n = all.size();

        int allThree = 0, oneCut = 0, twoCut = 0;
        int[] oddEven = new int[7]; // index = 奇数个数（0-6）
        int spanLt15 = 0, span15to19 = 0, span20to25 = 0, span26to32 = 0;
        int consecNone = 0, consec1 = 0, consec2 = 0, consec3 = 0, consec4 = 0, consec5 = 0;
        int blueOdd = 0, blueEven = 0;
        int repeat0 = 0, repeat1 = 0, repeat2 = 0, repeat3 = 0, repeatGt3 = 0;

        for (int i = 0; i < n; i++) {
            SsqNumber cur = all.get(i);
            int[] reds = cur.reds();

            // 空间分布
            int[] zone = new int[3];
            for (int r : reds) {
                zone[zoneOf(r)]++;
            }
            int emptyZones = 0;
            for (int z : zone) {
                if (z == 0) emptyZones++;
            }
            if (emptyZones == 0) allThree++;
            else if (emptyZones == 1) oneCut++;
            else twoCut++;

            // 奇偶
            int odd = StatisticsService.countOdd(reds);
            oddEven[odd]++;

            // 跨度
            int span = reds[5] - reds[0];
            if (span < 15) spanLt15++;
            else if (span <= 19) span15to19++;
            else if (span <= 25) span20to25++;
            else span26to32++;

            // 连号
            int maxRun = StatisticsService.maxConsecutiveRun(reds);
            int group = StatisticsService.consecutiveGroupCount(reds);
            if (maxRun <= 1) consecNone++;
            else if (maxRun == 5) consec5++;
            else if (maxRun == 4) consec4++;
            else if (maxRun == 3) consec3++;
            else {
                if (group >= 2) consec2++;
                else consec1++;
            }

            // 蓝球奇偶
            if ((cur.getBlue() & 1) == 1) blueOdd++;
            else blueEven++;

            // 重号（与上一期红球交集）
            if (i > 0) {
                int repeat = StatisticsService.intersectCount(reds, all.get(i - 1).reds());
                if (repeat == 0) repeat0++;
                else if (repeat == 1) repeat1++;
                else if (repeat == 2) repeat2++;
                else if (repeat == 3) repeat3++;
                else repeatGt3++;
            }
        }

        int repeatN = Math.max(0, n - 1);

        info.getZoneStats().add(statItem("三区都有号码", allThree, n, 79.92, 1.25));
        info.getZoneStats().add(statItem("至少断一区", oneCut + twoCut, n, 20.08, 4.98));
        info.getZoneStats().add(statItem("断两区(全落一区)", twoCut, n, 0.13, 799.11));

        info.getOddEvenStats().add(statItem("3奇3偶", oddEven[3], n, 34.38, 2.91));
        info.getOddEvenStats().add(statItem("4奇2偶", oddEven[4], n, 25.79, 3.88));
        info.getOddEvenStats().add(statItem("2奇4偶", oddEven[2], n, 22.35, 4.47));
        info.getOddEvenStats().add(statItem("5奇1偶", oddEven[5], n, 8.94, 11.19));
        info.getOddEvenStats().add(statItem("1奇5偶", oddEven[1], n, 6.70, 14.92));
        info.getOddEvenStats().add(statItem("6奇0偶", oddEven[6], n, 1.12, 89.49));
        info.getOddEvenStats().add(statItem("0奇6偶", oddEven[0], n, 0.72, 138.31));

        info.getSpanStats().add(statItem("跨度26-32", span26to32, n, 45.63, 2.19));
        info.getSpanStats().add(statItem("跨度20-25", span20to25, n, 37.22, 2.69));
        info.getSpanStats().add(statItem("跨度15-19", span15to19, n, 13.44, 7.44));
        info.getSpanStats().add(statItem("跨度<15", spanLt15, n, 3.71, 26.99));

        info.getRepeatStats().add(statItem("至少1个重号", repeat1 + repeat2 + repeat3 + repeatGt3, repeatN, 73.27, 1.36));
        info.getRepeatStats().add(statItem("无重号", repeat0, repeatN, 26.73, 3.74));
        info.getRepeatStats().add(statItem("1个重号", repeat1, repeatN, 43.73, 2.29));
        info.getRepeatStats().add(statItem("2个重号", repeat2, repeatN, 23.77, 4.21));
        info.getRepeatStats().add(statItem("3个重号", repeat3, repeatN, 5.28, 18.93));

        info.getConsecutiveStats().add(statItem("至少1组连号", consec1 + consec2 + consec3 + consec4 + consec5, n, 65.98, 1.52));
        info.getConsecutiveStats().add(statItem("1组连号", consec1, n, 52.72, 1.90));
        info.getConsecutiveStats().add(statItem("2组连号", consec2, n, 12.97, 7.71));
        info.getConsecutiveStats().add(statItem("3连号", consec3, n, 9.20, 10.87));
        info.getConsecutiveStats().add(statItem("4连号", consec4, n, 0.96, 104.65));
        info.getConsecutiveStats().add(statItem("5连号", consec5, n, 0.07, 1465));

        info.getBackOddEvenStats().add(statItem("蓝球奇数", blueOdd, n, 50.0, 2.0));
        info.getBackOddEvenStats().add(statItem("蓝球偶数", blueEven, n, 50.0, 2.0));
    }

    private void fillOmission(StatisticsInfo info, List<SsqNumber> allAsc) {
        List<SsqNumber> desc = new ArrayList<>(allAsc);
        Collections.reverse(desc);

        for (StatisticsInfo.StatItem item : info.getZoneStats()) {
            setOmit(item, desc, false);
        }
        for (StatisticsInfo.StatItem item : info.getOddEvenStats()) {
            setOmit(item, desc, false);
        }
        for (StatisticsInfo.StatItem item : info.getSpanStats()) {
            setOmit(item, desc, false);
        }
        for (StatisticsInfo.StatItem item : info.getRepeatStats()) {
            setOmit(item, desc, true);
        }
        for (StatisticsInfo.StatItem item : info.getConsecutiveStats()) {
            setOmit(item, desc, false);
        }
        for (StatisticsInfo.StatItem item : info.getBackOddEvenStats()) {
            setOmit(item, desc, false);
        }
    }

    private void setOmit(StatisticsInfo.StatItem item, List<SsqNumber> desc, boolean needPrev) {
        int omit = 0;
        for (int i = 0; i < desc.size(); i++) {
            SsqNumber cur = desc.get(i);
            SsqNumber prev = needPrev && (i + 1 < desc.size()) ? desc.get(i + 1) : null;
            if (matchFeature(item.getName(), cur, prev)) {
                omit = i;
                break;
            }
            omit = desc.size();
        }
        item.setOmit(omit);
        item.setExpecting(omit >= item.getTheoryPeriod());
    }

    private boolean matchFeature(String name, SsqNumber cur, SsqNumber prev) {
        int[] reds = cur.reds();
        int odd = StatisticsService.countOdd(reds);
        int span = reds[5] - reds[0];
        int maxRun = StatisticsService.maxConsecutiveRun(reds);
        int group = StatisticsService.consecutiveGroupCount(reds);
        int blue = cur.getBlue();

        switch (name) {
            case "三区都有号码":
                return coversThreeZone(reds);
            case "至少断一区":
                return !coversThreeZone(reds);
            case "断两区(全落一区)":
                return countNonEmptyZones(reds) == 1;
            // 奇偶
            case "3奇3偶": return odd == 3;
            case "4奇2偶": return odd == 4;
            case "2奇4偶": return odd == 2;
            case "5奇1偶": return odd == 5;
            case "1奇5偶": return odd == 1;
            case "6奇0偶": return odd == 6;
            case "0奇6偶": return odd == 0;
            // 跨度
            case "跨度26-32": return span >= 26 && span <= 32;
            case "跨度20-25": return span >= 20 && span <= 25;
            case "跨度15-19": return span >= 15 && span <= 19;
            case "跨度<15": return span < 15;
            // 重号
            case "至少1个重号": return prev != null && StatisticsService.intersectCount(reds, prev.reds()) >= 1;
            case "无重号": return prev != null && StatisticsService.intersectCount(reds, prev.reds()) == 0;
            case "1个重号": return prev != null && StatisticsService.intersectCount(reds, prev.reds()) == 1;
            case "2个重号": return prev != null && StatisticsService.intersectCount(reds, prev.reds()) == 2;
            case "3个重号": return prev != null && StatisticsService.intersectCount(reds, prev.reds()) == 3;
            // 连号
            case "至少1组连号": return maxRun >= 2;
            case "1组连号": return maxRun == 2 && group == 1;
            case "2组连号": return maxRun == 2 && group >= 2;
            case "3连号": return maxRun == 3;
            case "4连号": return maxRun == 4;
            case "5连号": return maxRun == 5;
            // 蓝球奇偶
            case "蓝球奇数": return (blue & 1) == 1;
            case "蓝球偶数": return (blue & 1) == 0;
            default:
                return false;
        }
    }

    private void fillFrequency(StatisticsInfo info, List<SsqNumber> all) {
        int window = Math.min(coldWindow, all.size());
        List<SsqNumber> recent = all.subList(all.size() - window, all.size());

        Map<Integer, Integer> redFreq = new HashMap<>();
        Map<Integer, Integer> blueFreq = new HashMap<>();
        for (SsqNumber s : recent) {
            for (int r : s.reds()) {
                redFreq.merge(r, 1, Integer::sum);
            }
            blueFreq.merge(s.getBlue(), 1, Integer::sum);
        }

        for (int num = 1; num <= 33; num++) {
            int freq = redFreq.getOrDefault(num, 0);
            int omit = computeOmit(recent, num, true);
            info.getFrontFreq().add(new StatisticsInfo.FreqItem(num, freq, omit));
        }
        for (int num = 1; num <= 16; num++) {
            int freq = blueFreq.getOrDefault(num, 0);
            int omit = computeOmit(recent, num, false);
            info.getBackFreq().add(new StatisticsInfo.FreqItem(num, freq, omit));
        }

        info.getFrontFreq().sort((a, b) -> {
            if (a.getFreq() != b.getFreq()) return Integer.compare(a.getFreq(), b.getFreq());
            return Integer.compare(b.getOmit(), a.getOmit());
        });
        info.getBackFreq().sort((a, b) -> {
            if (a.getFreq() != b.getFreq()) return Integer.compare(a.getFreq(), b.getFreq());
            return Integer.compare(b.getOmit(), a.getOmit());
        });
    }

    private int computeOmit(List<SsqNumber> recent, int num, boolean red) {
        for (int i = recent.size() - 1; i >= 0; i--) {
            int[] arr = recent.get(i).reds();
            boolean hit;
            if (red) {
                hit = StatisticsService.contains(arr, num);
            } else {
                hit = recent.get(i).getBlue() == num;
            }
            if (hit) {
                return recent.size() - 1 - i;
            }
        }
        return recent.size();
    }

    private boolean coversThreeZone(int[] reds) {
        boolean[] zone = new boolean[3];
        for (int r : reds) zone[zoneOf(r)] = true;
        return zone[0] && zone[1] && zone[2];
    }

    private int countNonEmptyZones(int[] reds) {
        boolean[] zone = new boolean[3];
        for (int r : reds) zone[zoneOf(r)] = true;
        int c = 0;
        for (boolean b : zone) if (b) c++;
        return c;
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
