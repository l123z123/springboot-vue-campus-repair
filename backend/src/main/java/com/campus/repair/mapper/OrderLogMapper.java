package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.OrderLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单操作日志 Mapper
 */
@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {
}
