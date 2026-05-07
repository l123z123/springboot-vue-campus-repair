package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.entity.SysFeedback;
import com.campus.repair.mapper.SysFeedbackMapper;
import com.campus.repair.service.FeedbackService;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    private final SysFeedbackMapper sysFeedbackMapper;

    public FeedbackServiceImpl(SysFeedbackMapper sysFeedbackMapper) {
        this.sysFeedbackMapper = sysFeedbackMapper;
    }

    @Override
    public void submitFeedback(Long userId, String content, String contactInfo) {
        SysFeedback f = new SysFeedback();
        f.setUserId(userId);
        f.setContent(content);
        f.setContactInfo(contactInfo);
        f.setStatus(0);
        f.setCreateTime(LocalDateTime.now());
        f.setUpdateTime(LocalDateTime.now());
        sysFeedbackMapper.insert(f);
    }

    @Override
    public List<SysFeedback> listMyFeedback(Long userId) {
        return sysFeedbackMapper.selectList(
                new LambdaQueryWrapper<SysFeedback>()
                        .eq(SysFeedback::getUserId, userId)
                        .orderByDesc(SysFeedback::getCreateTime)
        );
    }

    @Override
    public List<SysFeedback> listAllFeedback() {
        return sysFeedbackMapper.selectList(
                new LambdaQueryWrapper<SysFeedback>()
                        .orderByDesc(SysFeedback::getCreateTime)
        );
    }

    @Override
    public IPage<SysFeedback> pageAdmin(Page<SysFeedback> page, String keyword, Integer status) {
        LambdaQueryWrapper<SysFeedback> q = new LambdaQueryWrapper<SysFeedback>()
                .orderByDesc(SysFeedback::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            q.and(w -> w.like(SysFeedback::getContent, kw).or().like(SysFeedback::getContactInfo, kw));
        }
        if (status != null) {
            q.eq(SysFeedback::getStatus, status);
        }
        return sysFeedbackMapper.selectPage(page, q);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        SysFeedback f = sysFeedbackMapper.selectById(id);
        if (f == null) {
            return;
        }
        f.setStatus(status);
        f.setUpdateTime(LocalDateTime.now());
        sysFeedbackMapper.updateById(f);
    }
}

