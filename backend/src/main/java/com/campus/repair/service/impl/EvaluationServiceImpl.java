package com.campus.repair.service.impl;

import com.campus.repair.common.BusinessException;
import com.campus.repair.entity.RepairEvaluation;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.mapper.RepairEvaluationMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.service.EvaluationService;
import com.campus.repair.service.NotifyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 评价服务实现
 */
@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final RepairEvaluationMapper repairEvaluationMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final NotifyService notifyService;

    public EvaluationServiceImpl(RepairEvaluationMapper repairEvaluationMapper, RepairOrderMapper repairOrderMapper,
            NotifyService notifyService) {
        this.repairEvaluationMapper = repairEvaluationMapper;
        this.repairOrderMapper = repairOrderMapper;
        this.notifyService = notifyService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitEvaluation(Long orderId, Integer score, String comment, Integer isAnonymous) {
        // 检查工单是否存在
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }

        // 学生确认后、已评价前：与 RepairOrderStatus 状态机一致（7=学生确认 -> 8=已评价）
        int s = order.getStatus() != null ? order.getStatus() : -1;
        if (s != 7) {
            throw new BusinessException("请先在工单中确认维修完成后再评价");
        }

        // 检查是否已经评价过
        RepairEvaluation existingEvaluation = repairEvaluationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RepairEvaluation>()
                        .eq(RepairEvaluation::getOrderId, orderId)
        );
        if (existingEvaluation != null) {
            throw new BusinessException("该工单已经评价过");
        }

        // 创建评价
        RepairEvaluation evaluation = new RepairEvaluation();
        evaluation.setOrderId(orderId);
        evaluation.setScore(score);
        evaluation.setComment(comment);
        evaluation.setIsAnonymous(isAnonymous);
        evaluation.setIsDeleted(0);
        evaluation.setCreateTime(LocalDateTime.now());
        evaluation.setUpdateTime(LocalDateTime.now());

        // 保存评价
        repairEvaluationMapper.insert(evaluation);

        order.setStatus(8);
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);

        if (order.getRepairmanId() != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", orderId);
            payload.put("status", 8);
            payload.put("targetUserId", order.getRepairmanId());
            notifyService.sendRepairNotify("STATUS_CHANGED", payload);
        }
    }

    @Override
    public RepairEvaluation getByOrderId(Long orderId) {
        return repairEvaluationMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RepairEvaluation>()
                        .eq(RepairEvaluation::getOrderId, orderId)
        );
    }

}