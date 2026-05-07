package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
