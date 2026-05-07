package com.campus.repair.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.controller.dto.DispatchRecommendDTO;
import com.campus.repair.controller.dto.RepairCreateDTO;
import com.campus.repair.entity.RepairOrder;

import java.util.List;

/**
 * 报修业务
 */
public interface RepairService {

    RepairOrder create(RepairCreateDTO dto);

    /**
     * @param status    单状态过滤（与 statusIn 二选一，优先 statusIn）
     * @param statusIn  多状态 IN 查询，逗号分隔，如 0,1,3
     */
    IPage<RepairOrder> page(Page<RepairOrder> page, Integer status, List<Integer> statusIn, String keyword, String campus, String category, Integer urgency, String startDate, String endDate);

    RepairOrder getById(Long id);

    void updateStatus(Long orderId, Integer newStatus);
    
    /**
     * 更新工单状态，支持派单和备注
     */
    void updateStatusWithDetails(Long orderId, Integer newStatus, Long repairmanId, String remark);
    
    /**
     * 审核工单（管理员）
     */
    void auditOrder(Long orderId, boolean approved, String remark, Boolean assignToPool, Long assignRepairmanId);

    /**
     * 管理端：根据维修工负载、同类经验、同区域经验和评价给出派单推荐。
     */
    List<DispatchRecommendDTO> recommendRepairmen(Long orderId);
    
    /**
     * 派单给维修工（管理员）
     */
    void dispatchOrder(Long orderId, Long repairmanId);
    
    /**
     * 维修工接单（派单后开始维修）
     */
    void acceptOrder(Long orderId);
    
    /**
     * 维修工完成维修
     */
    void completeOrder(Long orderId);
    
    /**
     * 维修工完成维修（支持上传完成照片）
     */
    void completeOrderWithImages(Long orderId, String completedImages);
    
    /**
     * 学生确认维修完成
     */
    void confirmOrder(Long orderId);
}
