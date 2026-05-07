package com.campus.repair;

import com.campus.repair.controller.dto.RepairCreateDTO;
import com.campus.repair.context.UserContext;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.service.RepairService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 高并发提交工单单元测试：验证事务与数据正确写入
 */
@SpringBootTest
class RepairServiceConcurrentTest {

    @Autowired
    private RepairService repairService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 本测试在真实库中写入标题为「并发测试-…」的工单；子线程内独立事务会提交，与 @Transactional 无关。
     * 每次跑完测试后清掉，避免污染开发库 / 管理端列表。
     */
    @AfterEach
    void deleteConcurrentTestOrders() {
        jdbcTemplate.update(
                "DELETE e FROM repair_evaluation e INNER JOIN repair_order r ON e.order_id = r.order_id WHERE r.title LIKE '并发测试-%'");
        jdbcTemplate.update(
                "DELETE l FROM order_log l INNER JOIN repair_order r ON l.order_id = r.order_id WHERE r.title LIKE '并发测试-%'");
        jdbcTemplate.update(
                "DELETE c FROM chat_message c INNER JOIN repair_order r ON c.order_id = r.order_id WHERE r.title LIKE '并发测试-%'");
        jdbcTemplate.update("DELETE FROM repair_order WHERE title LIKE '并发测试-%'");
    }

    @Test
    void concurrentCreate_noDataLoss() throws Exception {
        int threadCount = 10;
        int submitsPerThread = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<RepairOrder>> futures = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final long userId = 1000L + t;
            for (int i = 0; i < submitsPerThread; i++) {
                final int idx = i;
                futures.add(executor.submit(() -> {
                    UserContext.set(userId, 0, "user" + userId);
                    try {
                        RepairCreateDTO dto = new RepairCreateDTO();
                        dto.setTitle("并发测试-" + userId + "-" + idx);
                        dto.setLocation("教学楼 A-" + userId);
                        dto.setUrgency(1);
                        dto.setPhoneNumber("1380000" + String.format("%04d", userId % 10000));
                        dto.setCampus("EAST");
                        dto.setArea("TECH");
                        dto.setCategory("LIFE_DORM");
                        return repairService.create(dto);
                    } finally {
                        UserContext.remove();
                    }
                }));
            }
        }

        int successCount = 0;
        List<Long> orderIds = new ArrayList<>();
        for (Future<RepairOrder> f : futures) {
            try {
                RepairOrder order = f.get(10, TimeUnit.SECONDS);
                assertNotNull(order);
                assertNotNull(order.getOrderId());
                orderIds.add(order.getOrderId());
                successCount++;
            } catch (ExecutionException e) {
                fail("提交失败: " + e.getCause().getMessage());
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(threadCount * submitsPerThread, successCount, "所有工单应成功写入");
        assertEquals(orderIds.size(), orderIds.stream().distinct().count(), "工单 ID 不应重复");
    }
}
