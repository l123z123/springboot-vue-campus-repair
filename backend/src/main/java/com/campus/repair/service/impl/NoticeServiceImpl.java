package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.entity.SysNotice;
import com.campus.repair.mapper.SysNoticeMapper;
import com.campus.repair.service.NoticeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NoticeServiceImpl implements NoticeService {

    private final SysNoticeMapper noticeMapper;

    public NoticeServiceImpl(SysNoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    @Override
    public IPage<SysNotice> listNotices(Page<SysNotice> page) {
        LambdaQueryWrapper<SysNotice> q = new LambdaQueryWrapper<>();
        q.orderByDesc(SysNotice::getPinned)
         .orderByDesc(SysNotice::getCreateTime);
        return noticeMapper.selectPage(page, q);
    }

    @Override
    public SysNotice publish(String title, String content, boolean pinned, String author) {
        SysNotice notice = new SysNotice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPinned(pinned);
        notice.setAuthor(author != null && !author.trim().isEmpty() ? author.trim() : "后勤管理处");
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);
        return notice;
    }

    @Override
    public void delete(Long id) {
        SysNotice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException("公告不存在");
        }
        noticeMapper.deleteById(id);
    }
}
