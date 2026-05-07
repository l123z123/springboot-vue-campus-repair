package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.enums.RepairOrderStatus;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.service.DashboardService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final RepairOrderMapper repairOrderMapper;

    public DashboardServiceImpl(RepairOrderMapper repairOrderMapper) {
        this.repairOrderMapper = repairOrderMapper;
    }

    @Override
    public Map<String, Object> getStats() {
        long todayCount = getTodayCount();
        long totalCount = getTotalCount();
        List<Map<String, Object>> topLocations = getLocationTop10();
        List<Map<String, Object>> statusDist = getStatusDist();

        Map<String, Object> result = new HashMap<>();
        result.put("todayCount", todayCount);
        result.put("totalCount", totalCount);
        // 向后兼容旧字段命名
        result.put("topLocations", topLocations);
        // 旧版本是 Map<String, Long>，这里改为 List 但保留一个可推导结构
        result.put("statusCount", statusDist.stream().collect(
                Collectors.toMap(m -> String.valueOf(m.get("name")), m -> (Long) m.get("value"))
        ));
        return result;
    }

    @Override
    public long getTodayCount() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        LambdaQueryWrapper<RepairOrder> q = new LambdaQueryWrapper<>();
        q.ge(RepairOrder::getCreateTime, todayStart).lt(RepairOrder::getCreateTime, todayEnd);

        return repairOrderMapper.selectCount(q);
    }

    @Override
    public long getTotalCount() {
        return repairOrderMapper.selectCount(null);
    }

    @Override
    public List<Map<String, Object>> getLocationTop10() {
        List<RepairOrder> all = repairOrderMapper.selectList(null);
        Map<String, Long> locationCount = all.stream()
                .filter(o -> o.getLocation() != null && !o.getLocation().trim().isEmpty())
                .collect(Collectors.groupingBy(RepairOrder::getLocation, Collectors.counting()));

        List<Map<String, Object>> topLocations = locationCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        return topLocations;
    }

    @Override
    public List<Map<String, Object>> getStatusDist() {
        List<RepairOrder> all = repairOrderMapper.selectList(null);
        Map<String, Long> statusCount = all.stream()
                .collect(Collectors.groupingBy(o -> String.valueOf(o.getStatus()), Collectors.counting()));

        return statusCount.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    try {
                        int code = Integer.parseInt(e.getKey());
                        RepairOrderStatus st = RepairOrderStatus.fromCode(code);
                        m.put("label", st != null ? st.getLabel() : e.getKey());
                    } catch (NumberFormatException ex) {
                        m.put("label", e.getKey());
                    }
                    m.put("value", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getCategoryTree() {
        List<RepairOrder> all = repairOrderMapper.selectList(null);
        // 大类编码 -> { 子类编码 -> 数量 }
        Map<String, Map<String, Long>> bucket = new HashMap<>();

        for (RepairOrder order : all) {
            String category = order.getCategory();
            if (category == null || category.trim().isEmpty()) {
                category = "OTHER";
            }
            String big = resolveBigCategory(category);
            bucket.computeIfAbsent(big, k -> new HashMap<>())
                    .merge(category, 1L, Long::sum);
        }

        return bucket.entrySet().stream().map(entry -> {
            String bigKey = entry.getKey();
            Map<String, Long> subMap = entry.getValue();
            Map<String, Object> node = new HashMap<>();
            node.put("name", bigCategoryLabel(bigKey));
            List<Map<String, Object>> children = subMap.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> child = new HashMap<>();
                        child.put("name", categoryLabel(e.getKey()));
                        child.put("value", e.getValue());
                        return child;
                    })
                    .collect(Collectors.toList());
            node.put("children", children);
            return node;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getHourHeatmap() {
        List<RepairOrder> all = repairOrderMapper.selectList(null);
        // key: day(0-6)-hour(0-23)
        Map<String, Long> counter = new HashMap<>();
        for (RepairOrder order : all) {
            LocalDateTime t = order.getCreateTime();
            if (t == null) continue;
            int day = t.getDayOfWeek().getValue() - 1; // 0=周一
            int hour = t.getHour();
            String key = day + "-" + hour;
            counter.merge(key, 1L, Long::sum);
        }
        long total = counter.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 24; h++) {
                String key = d + "-" + h;
                long v;
                if (total > 0) {
                    // 使用真实数据：若数据库中已有分布，则直接返回真实统计
                    v = counter.getOrDefault(key, 0L);
                } else {
                    // 否则使用更贴近日常的合成数据：
                    // 维修工工作时间 10:00-18:00，工作日(周一~周五)高于周末，午后略高
                    boolean workHour = h >= 10 && h <= 18;
                    boolean midday = h >= 13 && h <= 16;
                    boolean weekday = d >= 0 && d <= 4;
                    if (workHour) {
                        int base = weekday ? 4 : 2;
                        int bonus = midday ? 3 : 1;
                        v = base + bonus;
                    } else {
                        v = 0;
                    }
                }
                Map<String, Object> m = new HashMap<>();
                m.put("day", d);
                m.put("hour", h);
                m.put("value", v);
                result.add(m);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getStaffPerformance() {
        // 维修工绩效：已完工至闭环（维修完成 / 学生确认 / 已评价），与状态机 6/7/8 对齐
        LambdaQueryWrapper<RepairOrder> q = new LambdaQueryWrapper<>();
        q.isNotNull(RepairOrder::getRepairmanId)
                .in(RepairOrder::getStatus,
                        RepairOrderStatus.COMPLETED.getCode(),
                        RepairOrderStatus.CONFIRMED.getCode(),
                        RepairOrderStatus.CLOSED.getCode());
        List<RepairOrder> done = repairOrderMapper.selectList(q);
        Map<Long, Long> countMap = done.stream()
                .collect(Collectors.groupingBy(RepairOrder::getRepairmanId, Collectors.counting()));

        return countMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Long repairmanId = e.getKey();
                    Long cnt = e.getValue();
                    Map<String, Object> m = new HashMap<>();
                    m.put("repairmanId", repairmanId);
                    m.put("name", "维修员" + (repairmanId != null ? repairmanId : ""));
                    m.put("count", cnt);
                    // 占位评分，后续可从评价表计算
                    m.put("rating", 0);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getOverview() {
        long total = getTotalCount();
        // 待办：待审、审核中、待派单（与派单版状态机一致）
        LambdaQueryWrapper<RepairOrder> pendingQ = new LambdaQueryWrapper<>();
        pendingQ.in(RepairOrder::getStatus,
                RepairOrderStatus.SUBMITTED.getCode(),
                RepairOrderStatus.WAITING_AUDIT.getCode(),
                RepairOrderStatus.WAITING_DISPATCH.getCode());
        long pending = repairOrderMapper.selectCount(pendingQ);

        // 已走完维修闭环：维修完成 / 学生确认 / 已评价
        LambdaQueryWrapper<RepairOrder> completedQ = new LambdaQueryWrapper<>();
        completedQ.in(RepairOrder::getStatus,
                RepairOrderStatus.COMPLETED.getCode(),
                RepairOrderStatus.CONFIRMED.getCode(),
                RepairOrderStatus.CLOSED.getCode());
        long completed = repairOrderMapper.selectCount(completedQ);

        LambdaQueryWrapper<RepairOrder> urgentQ = new LambdaQueryWrapper<>();
        urgentQ.eq(RepairOrder::getUrgency, 3);
        long urgent = repairOrderMapper.selectCount(urgentQ);

        Map<String, Long> result = new HashMap<>();
        result.put("total", total);
        result.put("pending", pending);
        result.put("completed", completed);
        result.put("urgent", urgent);
        return result;
    }

    private String resolveBigCategory(String category) {
        if (category == null) return "OTHER";
        String c = category.toUpperCase(Locale.ROOT);
        if (c.startsWith("LIFE_")) {
            return "LIFE";
        }
        if (c.startsWith("INFRA_") || c.startsWith("INFRASTRUCTURE_")) {
            return "INFRA";
        }
        return "OTHER";
    }

    private String bigCategoryLabel(String key) {
        if ("LIFE".equals(key)) return "生活服务";
        if ("INFRA".equals(key)) return "基础设施";
        return "其他";
    }

    private String categoryLabel(String category) {
        if (category == null) return "其他";
        String c = category.toUpperCase(Locale.ROOT);
        switch (c) {
            case "LIFE_DORM":
                return "宿舍用电";
            case "LIFE_AIRCON":
                return "空调";
            case "LIFE_LIGHTING":
                return "照明";
            case "LIFE_WATER":
                return "给排水";
            case "LIFE_WASHER":
                return "洗衣设备";
            case "LIFE_CANTEEN":
            case "LIFE_CANTER":
                return "食堂餐厅";
            case "INFRA_BASKETBALL":
                return "球场设施";
            case "INFRA_TRACK":
                return "跑道/操场";
            case "TEACHING_LAB":
            case "TEACHING_LABORATORY":
                return "教学实验室";
            case "TEACHING_LAB_EQUIP":
            case "TEACHING_LAB_EQUIPMENT":
                return "教学实验室设备";
            case "TEACHING_MULTIMEDIA":
            case "TEACHING_MULTI_MEDIA":
                return "多媒体设备";
            default:
                // 兜底逻辑：任何包含 TEACHING 的都视为“教学相关设施”，INFRA* 视为“基础设施其他”
                if (c.contains("TEACHING")) {
                    return "教学相关设施";
                }
                if (c.startsWith("INFRA")) {
                    return "基础设施其他";
                }
                if (c.startsWith("LIFE_")) {
                    return "生活类其他";
                }
                return "其他";
        }
    }
}
