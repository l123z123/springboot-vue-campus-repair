package com.campus.repair.controller.admin;

import com.campus.repair.common.Result;
import com.campus.repair.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理员统计接口：提供给前端数据看板使用
 */
@RestController
@RequestMapping("/admin/stats")
public class StatsController {

    private final DashboardService dashboardService;

    public StatsController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 今日报修量
     */
    @GetMapping("/today")
    public Result<Long> today() {
        return Result.success(dashboardService.getTodayCount());
    }

    /**
     * 累计工单总数
     */
    @GetMapping("/total")
    public Result<Long> total() {
        return Result.success(dashboardService.getTotalCount());
    }

    /**
     * 区域报修 Top10
     */
    @GetMapping("/location-top10")
    public Result<List<Map<String, Object>>> locationTop10() {
        return Result.success(dashboardService.getLocationTop10());
    }

    /**
     * 工单状态分布
     */
    @GetMapping("/status-dist")
    public Result<List<Map<String, Object>>> statusDist() {
        return Result.success(dashboardService.getStatusDist());
    }

    /**
     * 故障类型分布（旭日图）
     */
    @GetMapping("/type-tree")
    public Result<List<Map<String, Object>>> typeTree() {
        return Result.success(dashboardService.getCategoryTree());
    }

    /**
     * 报修时段热力图
     */
    @GetMapping("/hour-heatmap")
    public Result<List<Map<String, Object>>> hourHeatmap() {
        return Result.success(dashboardService.getHourHeatmap());
    }

    /**
     * 维修工绩效榜
     */
    @GetMapping("/staff-performance")
    public Result<List<Map<String, Object>>> staffPerformance() {
        return Result.success(dashboardService.getStaffPerformance());
    }

    /**
     * 概览统计：total, pending, completed, urgent
     */
    @GetMapping("/overview")
    public Result<Map<String, Long>> overview() {
        return Result.success(dashboardService.getOverview());
    }
}

