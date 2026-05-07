package com.campus.repair.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.Result;
import com.campus.repair.controller.dto.DispatchRecommendDTO;
import com.campus.repair.controller.dto.RepairCreateDTO;
import com.campus.repair.controller.dto.RepairStatusDTO;
import com.campus.repair.controller.dto.EvaluationDTO;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.entity.RepairEvaluation;
import com.campus.repair.service.EvaluationService;
import com.campus.repair.annotation.OperationLog;
import com.campus.repair.service.RepairService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 报修工单接口
 */
@RestController
@RequestMapping("/repair")
public class RepairController {

    private final RepairService repairService;
    private final EvaluationService evaluationService;

    @PostMapping
    @OperationLog("submitRepair")
    public Result<RepairOrder> create(@Valid @RequestBody RepairCreateDTO dto) {
        RepairOrder order = repairService.create(dto);
        return Result.success(order);
    }

    @GetMapping("/list")
    public Result<IPage<RepairOrder>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String statusIn,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer urgency,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<RepairOrder> p = new Page<>(page, size);
        List<Integer> inList = null;
        if (StringUtils.hasText(statusIn)) {
            inList = new ArrayList<>();
            for (String part : statusIn.split(",")) {
                String t = part.trim();
                if (t.isEmpty()) continue;
                inList.add(Integer.parseInt(t));
            }
        }
        IPage<RepairOrder> data = repairService.page(p, status, inList, keyword, campus, category, urgency, startDate, endDate);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<RepairOrder> detail(@PathVariable Long id) {
        RepairOrder order = repairService.getById(id);
        return Result.success(order);
    }

    @PatchMapping("/{id}/status")
    @OperationLog("updateStatus")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody RepairStatusDTO dto) {
        repairService.updateStatus(id, dto.getStatus());
        return Result.success();
    }

    @PostMapping("/{id}/evaluation")
    @OperationLog("submitEvaluation")
    public Result<Void> submitEvaluation(@PathVariable Long id, @Valid @RequestBody EvaluationDTO dto) {
        evaluationService.submitEvaluation(id, dto.getScore(), dto.getComment(), dto.getIsAnonymous());
        return Result.success();
    }

    @GetMapping("/{id}/evaluation")
    public Result<RepairEvaluation> getEvaluation(@PathVariable Long id) {
        RepairEvaluation evaluation = evaluationService.getByOrderId(id);
        return Result.success(evaluation);
    }
    
    /**
     * 审核工单（管理员）
     */
    @PostMapping("/{id}/audit")
    @OperationLog("auditOrder")
    public Result<Void> auditOrder(@PathVariable Long id, @RequestBody AuditDTO dto) {
        repairService.auditOrder(
                id,
                dto.isApproved(),
                dto.getRemark(),
                dto.getAssignToPool(),
                dto.getAssignRepairmanId()
        );
        return Result.success();
    }
    
    /**
     * 派单给维修工（管理员）
     */
    @PostMapping("/{id}/dispatch")
    @OperationLog("dispatchOrder")
    public Result<Void> dispatchOrder(@PathVariable Long id, @RequestBody DispatchDTO dto) {
        repairService.dispatchOrder(id, dto.getRepairmanId());
        return Result.success();
    }
    
    @GetMapping("/{id}/dispatch-recommendations")
    public Result<List<DispatchRecommendDTO>> dispatchRecommendations(@PathVariable Long id) {
        return Result.success(repairService.recommendRepairmen(id));
    }
    
    /**
     * 维修工接单（旧派单模式，保留兼容）
     */
    @PostMapping("/{id}/accept")
    @OperationLog("acceptOrder")
    public Result<Void> acceptOrder(@PathVariable Long id) {
        repairService.acceptOrder(id);
        return Result.success();
    }
    
    /**
     * 维修工完成维修
     */
    @PostMapping("/{id}/complete")
    @OperationLog("completeOrder")
    public Result<Void> completeOrder(@PathVariable Long id) {
        repairService.completeOrder(id);
        return Result.success();
    }
    
    /**
     * 维修工完成维修（支持上传完成照片）
     */
    @PostMapping("/{id}/complete-with-images")
    @OperationLog("completeOrderWithImages")
    public Result<Void> completeOrderWithImages(@PathVariable Long id, @RequestBody CompleteOrderDTO dto) {
        repairService.completeOrderWithImages(id, dto.getCompletedImages());
        return Result.success();
    }
    
    public static class CompleteOrderDTO {
        private String completedImages;
        
        public String getCompletedImages() {
            return completedImages;
        }
        
        public void setCompletedImages(String completedImages) {
            this.completedImages = completedImages;
        }
    }
    
    /**
     * 学生确认维修完成
     */
    @PostMapping("/{id}/confirm")
    @OperationLog("confirmOrder")
    public Result<Void> confirmOrder(@PathVariable Long id) {
        repairService.confirmOrder(id);
        return Result.success();
    }
    
    public RepairController(RepairService repairService, EvaluationService evaluationService) {
        this.repairService = repairService;
        this.evaluationService = evaluationService;
    }
    
    // 审核DTO：通过时可先进入待派单，也可直接指定维修工
    public static class AuditDTO {
        private boolean approved;
        private String remark;
        /** 兼容旧前端字段；派单版不再使用该字段，后端以 assignRepairmanId 是否为空决定是否直派 */
        private Boolean assignToPool;
        private Long assignRepairmanId;
        
        public boolean isApproved() {
            return approved;
        }
        
        public void setApproved(boolean approved) {
            this.approved = approved;
        }
        
        public String getRemark() {
            return remark;
        }
        
        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Boolean getAssignToPool() {
            return assignToPool;
        }

        public void setAssignToPool(Boolean assignToPool) {
            this.assignToPool = assignToPool;
        }

        public Long getAssignRepairmanId() {
            return assignRepairmanId;
        }

        public void setAssignRepairmanId(Long assignRepairmanId) {
            this.assignRepairmanId = assignRepairmanId;
        }
    }
    
    // 派单DTO
    public static class DispatchDTO {
        private Long repairmanId;
        
        public Long getRepairmanId() {
            return repairmanId;
        }
        
        public void setRepairmanId(Long repairmanId) {
            this.repairmanId = repairmanId;
        }
    }
}
