package com.campus.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.common.Result;
import com.campus.repair.context.UserContext;
import com.campus.repair.controller.dto.PageResult;
import com.campus.repair.entity.SysNotice;
import com.campus.repair.service.NoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/notice/list")
    public Result<PageResult<SysNotice>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Page<SysNotice> p = new Page<>(page, size);
        IPage<SysNotice> iPage = noticeService.listNotices(p);
        return Result.success(PageResult.from(iPage));
    }

    @PostMapping("/admin/notice")
    public Result<SysNotice> publish(@RequestBody Map<String, Object> body) {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "仅管理员可发布公告");
        }
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        Boolean pinned = (Boolean) body.getOrDefault("pinned", false);
        String author = (String) body.get("author");

        if (title == null || title.trim().isEmpty()) {
            throw new BusinessException("公告标题不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("公告内容不能为空");
        }
        if (title.trim().length() > 100) {
            throw new BusinessException("公告标题不能超过100字");
        }
        if (content.trim().length() > 500) {
            throw new BusinessException("公告内容不能超过500字");
        }

        SysNotice notice = noticeService.publish(title.trim(), content.trim(),
                pinned != null && pinned, author);
        return Result.success(notice);
    }

    @DeleteMapping("/admin/notice/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Integer role = UserContext.getRole();
        if (role == null || role != 2) {
            throw new BusinessException(403, "仅管理员可删除公告");
        }
        noticeService.delete(id);
        return Result.success();
    }
}
