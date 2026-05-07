package com.campus.repair.service;

import java.util.List;
import java.util.Map;

/**
 * 数据看板服务：今日报修量、区域分布、状态分布等聚合数据
 */
public interface DashboardService {

    /**
     * 综合统计（向后兼容原有 /admin/dashboard/stats 接口）
     */
    Map<String, Object> getStats();

    /**
     * 今日报修量
     */
    long getTodayCount();

    /**
     * 累计工单总数
     */
    long getTotalCount();

    /**
     * 区域报修 Top10，元素为 {name, value}
     */
    List<Map<String, Object>> getLocationTop10();

    /**
     * 工单状态分布，元素为 {name, value}
     */
    List<Map<String, Object>> getStatusDist();

    /**
     * 故障类型分布（旭日图）：
     * [
     *   { name: '生活服务', children: [ { name: '宿舍用电', value: 10 }, ... ] },
     *   { name: '基础设施', children: [ ... ] }
     * ]
     */
    List<Map<String, Object>> getCategoryTree();

    /**
     * 报修时段热力图：一周 7 天 * 24 小时
     * 每个元素为 { day: 0-6(周一到周日), hour: 0-23, value: count }
     */
    List<Map<String, Object>> getHourHeatmap();

    /**
     * 维修工绩效榜：每个元素为 { repairmanId, name, count, rating }
     * rating 暂时为占位字段（可与评价系统对接）
     */
    List<Map<String, Object>> getStaffPerformance();

    /**
     * 概览统计：总工单、待处理(status=0)、已完成(status=2)、紧急工单(urgency=3)
     */
    Map<String, Long> getOverview();
}
