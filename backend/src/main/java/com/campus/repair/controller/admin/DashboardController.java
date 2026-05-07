package com.campus.repair.controller.admin;

import com.campus.repair.common.Result;
import com.campus.repair.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员数据看板（需 ADMIN 角色，SecurityConfig /admin/**）
 */
@RestController
@RequestMapping("/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(dashboardService.getStats());
    }
}
