package com.campus.repair.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.service.NotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SLA 超时监控：只提醒管理员，不自动改状态，避免破坏人工派单与维修流程。
 */
@Component
public class SlaTimeoutMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaTimeoutMonitor.class);

    private static final int STATUS_SUBMITTED = 0;
    private static final int STATUS_WAITING_AUDIT = 1;
    private static final int STATUS_WAITING_DISPATCH = 3;
    private static final int STATUS_DISPATCHED = 4;
    private static final int STATUS_PROCESSING = 5;

    private final RepairOrderMapper repairOrderMapper;
    private final NotifyService notifyService;

    @Value("${repair.sla.pending-hours:24}")
    private int pendingHours;

    public SlaTimeoutMonitor(RepairOrderMapper repairOrderMapper, NotifyService notifyService) {
        this.repairOrderMapper = repairOrderMapper;
        this.notifyService = notifyService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void scanTimeoutOrders() {
        LocalDateTime before = LocalDateTime.now().minusHours(pendingHours);
        List<Integer> statuses = Arrays.asList(
                STATUS_SUBMITTED,
                STATUS_WAITING_AUDIT,
                STATUS_WAITING_DISPATCH,
                STATUS_DISPATCHED,
                STATUS_PROCESSING
        );
        LambdaQueryWrapper<RepairOrder> q = new LambdaQueryWrapper<>();
        q.in(RepairOrder::getStatus, statuses)
                .lt(RepairOrder::getCreateTime, before);
        List<RepairOrder> list = repairOrderMapper.selectList(q);
        if (list == null || list.isEmpty()) {
            return;
        }

        String ids = list.stream().map(RepairOrder::getOrderId).map(String::valueOf).collect(Collectors.joining(","));
        log.warn("[SLA] 超过 {} 小时未闭环的工单数: {}，orderIds: {}", pendingHours, list.size(), ids);

        Map<String, Object> payload = new HashMap<>();
        payload.put("count", list.size());
        payload.put("orderIds", list.stream().map(RepairOrder::getOrderId).collect(Collectors.toList()));
        payload.put("message", "存在工单处理超时风险，请管理员关注派单与进度");
        notifyService.sendRepairNotify("SLA_TIMEOUT", payload);
    }
}
