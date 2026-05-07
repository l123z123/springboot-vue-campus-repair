package com.campus.repair.service;

import java.util.Map;

/**
 * 实时通知服务：工单创建、状态变更时推送
 */
public interface NotifyService {
    void sendRepairNotify(String type, Map<String, Object> payload);
}
