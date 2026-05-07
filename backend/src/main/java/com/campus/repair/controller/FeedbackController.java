package com.campus.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import com.campus.repair.context.UserContext;
import com.campus.repair.controller.dto.PageResult;
import com.campus.repair.entity.SysFeedback;
import com.campus.repair.service.FeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/feedback/submit")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, String> dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        String content = dto.get("content");
        String contactInfo = dto.get("contactInfo");

        if (content == null || content.trim().length() < 5 || content.trim().length() > 500) {
            throw new BusinessException("反馈内容需在 5-500 字之间");
        }

        if (contactInfo != null && !contactInfo.trim().isEmpty()) {
            String v = contactInfo.trim();
            boolean phoneOk = v.matches("^1[3-9]\\d{9}$");
            boolean emailOk = v.matches("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$");
            if (!phoneOk && !emailOk) {
                throw new BusinessException("请输入有效的手机号或邮箱地址");
            }
            contactInfo = v;
        } else {
            contactInfo = null;
        }

        feedbackService.submitFeedback(userId, content.trim(), contactInfo);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "提交成功");
        return Result.success(resp);
    }

    @GetMapping("/feedback/my-list")
    public Result<List<SysFeedback>> myList() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return Result.success(feedbackService.listMyFeedback(userId));
    }

    @GetMapping("/admin/feedback/list")
    public Result<PageResult<SysFeedback>> adminList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "仅管理员可查看反馈列表");
        }
        Page<SysFeedback> p = new Page<>(page, size);
        IPage<SysFeedback> iPage = feedbackService.pageAdmin(p, keyword, status);
        return Result.success(PageResult.from(iPage));
    }

    @PutMapping("/admin/feedback/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "仅管理员可操作反馈状态");
        }
        Integer status = body.getOrDefault("status", 1);
        feedbackService.updateStatus(id, status);
        return Result.success();
    }
}

