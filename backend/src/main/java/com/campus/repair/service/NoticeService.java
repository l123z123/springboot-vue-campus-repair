package com.campus.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.entity.SysNotice;

import java.util.List;

public interface NoticeService {

    IPage<SysNotice> listNotices(Page<SysNotice> page);

    SysNotice publish(String title, String content, boolean pinned, String author);

    void delete(Long id);
}
