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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 超时工单自动取消：超过配置时间的待处理工单自动标记为已取消并通知管理员。
 */
@Component
public class SlaTimeoutMonitor {

    private static final Logger log = LoggerFactory.getLogger(SlaTimeoutMonitor.class);

    private static final int STATUS_SUBMITTED = 0;
    private static final int STATUS_WAITING_AUDIT = 1;
    private static final int STATUS_WAITING_DISPATCH = 3;
    private static final int STATUS_CANCELLED = 10;

    private final RepairOrderMapper repairOrderMapper;
    private final NotifyService notifyService;

    @Value("${repair.sla.pending-hours:24}")
    private int pendingHours;

    public SlaTimeoutMonitor(RepairOrderMapper repairOrderMapper, NotifyService notifyService) {
        this.repairOrderMapper = repairOrderMapper;
        this.notifyService = notifyService;
    }

    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void scanTimeoutOrders() {
        LocalDateTime before = LocalDateTime.now().minusHours(pendingHours);
        List<Integer> statuses = Arrays.asList(
                STATUS_SUBMITTED,
                STATUS_WAITING_AUDIT,
                STATUS_WAITING_DISPATCH
        );
        LambdaQueryWrapper<RepairOrder> q = new LambdaQueryWrapper<>();
        q.in(RepairOrder::getStatus, statuses)
                .lt(RepairOrder::getCreateTime, before);
        List<RepairOrder> list = repairOrderMapper.selectList(q);
        if (list == null || list.isEmpty()) {
            return;
        }

        for (RepairOrder order : list) {
            order.setStatus(STATUS_CANCELLED);
            order.setUpdateTime(LocalDateTime.now());
            order.setRemark("系统自动取消：超过 " + pendingHours + " 小时未处理");
            repairOrderMapper.updateById(order);
            log.info("[SLA] 自动取消超时工单: orderId={}, createTime={}", order.getOrderId(), order.getCreateTime());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("count", list.size());
        payload.put("message", "系统已自动取消 " + list.size() + " 个超时未处理的工单（超过" + pendingHours + "小时）");
        notifyService.sendRepairNotify("SLA_TIMEOUT", payload);
    }
}
