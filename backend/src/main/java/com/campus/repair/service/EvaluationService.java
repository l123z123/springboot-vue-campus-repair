package com.campus.repair.service;

import com.campus.repair.entity.RepairEvaluation;

/**
 * 评价服务
 */
public interface EvaluationService {

    /**
     * 提交评价
     */
    void submitEvaluation(Long orderId, Integer score, String comment, Integer isAnonymous);

    /**
     * 根据工单ID获取评价
     */
    RepairEvaluation getByOrderId(Long orderId);

}