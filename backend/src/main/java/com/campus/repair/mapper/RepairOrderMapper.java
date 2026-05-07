package com.campus.repair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.repair.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报修工单 Mapper
 */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {
}
