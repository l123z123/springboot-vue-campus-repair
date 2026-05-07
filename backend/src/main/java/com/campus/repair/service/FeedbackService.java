package com.campus.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.entity.SysFeedback;

import java.util.List;

public interface FeedbackService {

    void submitFeedback(Long userId, String content, String contactInfo);

    List<SysFeedback> listMyFeedback(Long userId);

    List<SysFeedback> listAllFeedback();

    IPage<SysFeedback> pageAdmin(Page<SysFeedback> page, String keyword, Integer status);

    void updateStatus(Long id, Integer status);
}

