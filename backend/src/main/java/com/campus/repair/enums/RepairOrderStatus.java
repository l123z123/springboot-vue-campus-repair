package com.campus.repair.enums;

import java.util.HashSet;
import java.util.Set;

/**
 * 工单状态机：定义合法流转（管理员派单模式）
 * SUBMITTED(0) -> WAITING_AUDIT(1) -> WAITING_DISPATCH(3) -> DISPATCHED(4) -> PROCESSING(5) -> COMPLETED(6) -> CONFIRMED(7) -> CLOSED(8)
 *                |                    |                       |
 *                +-> REJECTED(9)     +-> CANCELLED(10)       +-> CANCELLED(10)
 *
 * 完整的校园报修业务流程（派单模式）：
 * 1. SUBMITTED - 学生提交报修
 * 2. WAITING_AUDIT - 待审核
 * 3. WAITING_DISPATCH - 待派单（管理员审核通过后等待派单）
 * 4. DISPATCHED - 已派单（绑定维修工）
 * 5. PROCESSING - 维修中
 * 6. COMPLETED - 维修工完成维修
 * 7. CONFIRMED - 学生确认完成
 * 8. CLOSED - 已评价
 * 9. REJECTED - 已拒绝
 * 10. CANCELLED - 已取消
 */
public enum RepairOrderStatus {
    SUBMITTED(0, "待审核", new HashSet<Integer>() {{ add(1); add(10); }}),
    WAITING_AUDIT(1, "审核中", new HashSet<Integer>() {{ add(3); add(4); add(9); }}),
    AUDITED(2, "已审核", new HashSet<Integer>() {{ add(3); add(4); add(10); }}),
    WAITING_DISPATCH(3, "待派单", new HashSet<Integer>() {{ add(4); add(10); }}),
    DISPATCHED(4, "已派单", new HashSet<Integer>() {{ add(5); add(10); }}),
    PROCESSING(5, "维修中", new HashSet<Integer>() {{ add(6); }}),
    COMPLETED(6, "维修完成", new HashSet<Integer>() {{ add(7); }}),
    CONFIRMED(7, "学生确认", new HashSet<Integer>() {{ add(8); }}),
    CLOSED(8, "已评价", new HashSet<Integer>()),
    REJECTED(9, "已拒绝", new HashSet<Integer>()),
    CANCELLED(10, "已取消", new HashSet<Integer>());

    private final int code;
    private final String label;
    private final Set<Integer> allowedNext;

    RepairOrderStatus(int code, String label, Set<Integer> allowedNext) {
        this.code = code;
        this.label = label;
        this.allowedNext = allowedNext;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean canTransitionTo(int newCode) {
        return allowedNext.contains(newCode);
    }

    public static RepairOrderStatus fromCode(int code) {
        for (RepairOrderStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
