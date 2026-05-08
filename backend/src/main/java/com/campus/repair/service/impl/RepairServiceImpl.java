package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.context.UserContext;
import com.campus.repair.controller.dto.DispatchRecommendDTO;
import com.campus.repair.controller.dto.RepairCreateDTO;
import com.campus.repair.entity.RepairEvaluation;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.RepairEvaluationMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.enums.RepairOrderStatus;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.service.NotifyService;
import com.campus.repair.service.RepairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 报修业务实现：防重、分页、状态流转、WebSocket 通知
 */
@Service
public class RepairServiceImpl implements RepairService {

    private static final Logger log = LoggerFactory.getLogger(RepairServiceImpl.class);

    private static final String SUBMIT_LOCK_PREFIX = "submit:lock:";
    private static final String ORDER_LOCK_PREFIX = "lock:order:";
    private static final int SUBMIT_LOCK_SECONDS = 5;
    private static final int ORDER_LOCK_SECONDS = 10;
    
    // 完整的工单状态定义（管理员派单模式）
    private static final int STATUS_SUBMITTED = 0;
    private static final int STATUS_WAITING_AUDIT = 1;
    private static final int STATUS_AUDITED = 2;
    private static final int STATUS_WAITING_DISPATCH = 3;
    private static final int STATUS_DISPATCHED = 4;
    private static final int STATUS_PROCESSING = 5;
    private static final int STATUS_COMPLETED = 6;
    private static final int STATUS_CONFIRMED = 7;
    private static final int STATUS_CLOSED = 8;
    private static final int STATUS_REJECTED = 9;
    private static final int STATUS_CANCELLED = 10;
    
    // 为兼容旧代码的别名
    private static final int STATUS_PENDING = STATUS_SUBMITTED;
    private static final int STATUS_DONE = STATUS_COMPLETED;

    private final RepairOrderMapper repairOrderMapper;
    private final StringRedisTemplate redisTemplate;
    private final NotifyService notifyService;
    private final SysUserMapper sysUserMapper;
    private final RepairEvaluationMapper repairEvaluationMapper;

    public RepairServiceImpl(RepairOrderMapper repairOrderMapper,
                             StringRedisTemplate redisTemplate,
                             NotifyService notifyService,
                             SysUserMapper sysUserMapper,
                             RepairEvaluationMapper repairEvaluationMapper) {
        this.repairOrderMapper = repairOrderMapper;
        this.redisTemplate = redisTemplate;
        this.notifyService = notifyService;
        this.sysUserMapper = sysUserMapper;
        this.repairEvaluationMapper = repairEvaluationMapper;
    }

    private static boolean idEquals(Long a, Long b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.longValue() == b.longValue();
    }

    /** 与 JWT/库中 Integer 角色比较，避免非常规类型导致 != 误判 */
    private static boolean isStudentRole(Integer role) {
        return role != null && role.intValue() == 0;
    }

    private static boolean isRepairRole(Integer role) {
        return role != null && role.intValue() == 1;
    }

    private static boolean isAdminRole(Integer role) {
        return role != null && role.intValue() == 2;
    }

    /**
     * 校验并返回可用于派单的维修工账号（存在、角色正确、启用、且非报修人本人）
     */
    private SysUser validateAssignableRepairman(Long repairmanId, Long orderUserId) {
        if (repairmanId == null) {
            throw new BusinessException(400, "指定派单时必须选择维修工");
        }
        SysUser w = sysUserMapper.selectById(repairmanId);
        if (w == null) {
            throw new BusinessException(400, "维修工不存在或已删除");
        }
        if (!isRepairRole(w.getRole())) {
            throw new BusinessException(400, "请选择「维修工」角色账号进行派单");
        }
        if (w.getStatus() != null && w.getStatus() == 0) {
            throw new BusinessException(400, "该维修工账号已停用，请更换");
        }
        if (idEquals(orderUserId, repairmanId)) {
            throw new BusinessException(400, "报修人本人不能作为本单维修工");
        }
        return w;
    }

    /**
     * 与 {@link #page} 中维修工列表规则一致，避免 getById 能看列表、打开详情却 403；并兼容主键/外键 Jdbc 类型差异
     */
    private boolean staffMatchesListRule(RepairOrder order, Long me) {
        if (order == null || me == null) {
            return false;
        }
        Integer s = order.getStatus();
        if (s == null) {
            return false;
        }
        if (idEquals(order.getRepairmanId(), me)) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RepairOrder create(RepairCreateDTO dto) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        String phone = dto.getPhoneNumber();
        if (!StringUtils.hasText(phone) || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("请输入有效的手机号码");
        }
        if (!StringUtils.hasText(dto.getCampus()) || !StringUtils.hasText(dto.getArea()) || !StringUtils.hasText(dto.getCategory())) {
            throw new BusinessException("请选择校区与报修种类");
        }
        String location = dto.getLocation();
        if (!StringUtils.hasText(location)) {
            StringBuilder sb = new StringBuilder();
            sb.append(dto.getCampus()).append("-").append(dto.getArea());
            if (StringUtils.hasText(dto.getLocationDetail())) {
                sb.append("-").append(dto.getLocationDetail());
            }
            location = sb.toString();
        }
        long ts = System.currentTimeMillis();
        log.info("create ticket, userId={}, location={}, timestamp={}", userId, location, ts);

        String lockKey = SUBMIT_LOCK_PREFIX + userId + ":" + (ts / 1000);
        try {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", SUBMIT_LOCK_SECONDS, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(locked)) {
                throw new BusinessException("请勿重复提交");
            }
        } catch (Exception e) {
            log.warn("Redis 防重校验失败，继续提交: {}", e.getMessage());
        }
        
        RepairOrder order = new RepairOrder();
        order.setUserId(userId);
        order.setTitle(dto.getTitle());
        order.setDescription(dto.getDescription());
        order.setLocation(location);
        order.setUrgency(dto.getUrgency() != null ? dto.getUrgency() : 2);
        order.setIsUrgent(dto.getIsUrgent() != null ? dto.getIsUrgent() : false);
        
        // 一键急诊：跳过审核，直接进入已派单状态，让维修工可以直接接单
        if (Boolean.TRUE.equals(order.getIsUrgent())) {
            log.info("触发一键急诊流程: 跳过审核直接进入待派单");
            order.setStatus(STATUS_WAITING_DISPATCH);
            order.setAuditTime(LocalDateTime.now());
        } else {
            // 普通报修：待审核状态，需要管理员审核后才能派单
            order.setStatus(STATUS_SUBMITTED);
        }
        
        order.setImages(dto.getImages());
        order.setPhoneNumber(phone);
        order.setCampus(dto.getCampus());
        order.setArea(dto.getArea());
        order.setCategory(dto.getCategory());
        
        int rows = repairOrderMapper.insert(order);
        if (rows <= 0) {
            throw new BusinessException("工单创建失败");
        }
        log.info("created orderId={}, userId={}, location={}, isUrgent={}", 
            order.getOrderId(), userId, location, order.getIsUrgent());
        
        Map<String, Object> notifyPayload = new HashMap<>();
        notifyPayload.put("orderId", order.getOrderId());
        notifyPayload.put("userId", userId);
        notifyPayload.put("location", location);
        notifyPayload.put("phoneNumber", phone);
        notifyPayload.put("campus", dto.getCampus());
        notifyPayload.put("area", dto.getArea());
        notifyPayload.put("category", dto.getCategory());
        notifyPayload.put("status", order.getStatus());
        notifyPayload.put("isUrgent", order.getIsUrgent());
        notifyService.sendRepairNotify("CREATED", notifyPayload);

        return order;
    }

    @Override
    public IPage<RepairOrder> page(Page<RepairOrder> page, Integer status, List<Integer> statusIn, String keyword, String campus, String category, Integer urgency, String startDate, String endDate) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) {
            throw new BusinessException(401, "请先登录");
        }
        LambdaQueryWrapper<RepairOrder> q = new LambdaQueryWrapper<>();
        if (isStudentRole(role)) {
            // 学生：只能看到自己的工单
            q.eq(RepairOrder::getUserId, currentId);
        } else if (isRepairRole(role)) {
            // 维修工：仅查看管理员派给自己的工单，符合校园后勤统一调度模式
            q.eq(RepairOrder::getRepairmanId, currentId);
        }
        // 管理员：可以看到所有工单
        if (statusIn != null && !statusIn.isEmpty()) {
            q.in(RepairOrder::getStatus, statusIn);
        } else if (status != null) {
            q.eq(RepairOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(RepairOrder::getTitle, keyword).or().like(RepairOrder::getLocation, keyword));
        }
        if (StringUtils.hasText(campus)) {
            q.eq(RepairOrder::getCampus, campus);
        }
        if (StringUtils.hasText(category)) {
            q.eq(RepairOrder::getCategory, category);
        }
        if (urgency != null) {
            q.eq(RepairOrder::getUrgency, urgency);
        }
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                q.ge(RepairOrder::getCreateTime, start);
            } catch (Exception ignored) {
                // 日期格式异常则忽略
            }
        }
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                q.le(RepairOrder::getCreateTime, end);
            } catch (Exception ignored) {
                // 日期格式异常则忽略
            }
        }
        q.orderByDesc(RepairOrder::getCreateTime);
        IPage<RepairOrder> out = repairOrderMapper.selectPage(page, q);
        enrichUserNamesForList(out.getRecords());
        return out;
    }

    /** 列表页填充报修人/维修工姓名，批量查询避免 N+1 */
    private void enrichUserNamesForList(List<RepairOrder> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = new ArrayList<>();
        for (RepairOrder o : records) {
            if (o.getUserId() != null) userIds.add(o.getUserId());
            if (o.getRepairmanId() != null) userIds.add(o.getRepairmanId());
        }
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserMapper.selectBatchIds(userIds.stream().distinct().collect(Collectors.toList()));
            for (SysUser u : users) {
                userMap.put(u.getUserId(), u);
            }
        }
        for (RepairOrder order : records) {
            SysUser student = order.getUserId() != null ? userMap.get(order.getUserId()) : null;
            if (student != null) {
                order.setStudentName(firstName(student.getRealName(), student.getNickname(), student.getUsername()));
            }
            SysUser worker = order.getRepairmanId() != null ? userMap.get(order.getRepairmanId()) : null;
            if (worker != null) {
                order.setRepairmanName(firstName(worker.getRealName(), worker.getNickname(), worker.getUsername()));
            }
        }
    }

    @Override
    public RepairOrder getById(Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (isAdminRole(role)) {
            enrichUserNamesForDetail(order);
            return order;
        }
        if (isRepairRole(role) && staffMatchesListRule(order, currentId)) {
            enrichUserNamesForDetail(order);
            return order;
        }
        if (idEquals(order.getUserId(), currentId) || idEquals(order.getRepairmanId(), currentId)) {
            enrichUserNamesForDetail(order);
            return order;
        }
        throw new BusinessException(403, "无权限查看该工单");
    }

    private void enrichUserNamesForDetail(RepairOrder order) {
        List<Long> ids = new ArrayList<>();
        if (order.getUserId() != null) ids.add(order.getUserId());
        if (order.getRepairmanId() != null) ids.add(order.getRepairmanId());
        if (ids.isEmpty()) return;
        List<SysUser> users = sysUserMapper.selectBatchIds(ids);
        Map<Long, SysUser> userMap = new HashMap<>();
        for (SysUser u : users) {
            userMap.put(u.getUserId(), u);
        }
        SysUser student = order.getUserId() != null ? userMap.get(order.getUserId()) : null;
        if (student != null) {
            order.setStudentName(firstName(student.getRealName(), student.getNickname(), student.getUsername()));
        }
        SysUser worker = order.getRepairmanId() != null ? userMap.get(order.getRepairmanId()) : null;
        if (worker != null) {
            order.setRepairmanName(firstName(worker.getRealName(), worker.getNickname(), worker.getUsername()));
        }
    }

    private static String firstName(String a, String b, String c) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        if (StringUtils.hasText(b)) {
            return b;
        }
        if (c != null && !c.isEmpty()) {
            return c;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long orderId, Integer newStatus) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) throw new BusinessException(401, "请先登录");

        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");

        int oldStatus = order.getStatus();
        if (oldStatus == newStatus) return;

        RepairOrderStatus from = RepairOrderStatus.fromCode(oldStatus);
        if (from == null || !from.canTransitionTo(newStatus)) {
            throw new BusinessException(400, "状态流转非法");
        }

        if (newStatus == STATUS_PROCESSING && oldStatus == STATUS_PENDING) {
            if (!isRepairRole(role)) {
                throw new BusinessException(403, "仅维修员可接单");
            }
            
            // 维修工并发接单限制 (<=3单)
            LambdaQueryWrapper<RepairOrder> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.eq(RepairOrder::getRepairmanId, currentId)
                        .eq(RepairOrder::getStatus, STATUS_PROCESSING);
            Long activeCount = repairOrderMapper.selectCount(countWrapper);
            if (activeCount != null && activeCount >= 3) {
                throw new BusinessException(400, "当前处理中工单已达上限(3单)，请先完成手中工单");
            }

            String lockKey = ORDER_LOCK_PREFIX + orderId;
            Boolean locked = null;
            try {
                locked = redisTemplate.opsForValue().setIfAbsent(lockKey, currentId.toString(), ORDER_LOCK_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis 接单锁校验失败，降级放行: {}", e.getMessage());
            }
            if (Boolean.FALSE.equals(locked)) {
                throw new BusinessException("手慢了，订单已被抢");
            }
            try {
                order.setRepairmanId(currentId);
                order.setStatus(STATUS_PROCESSING);
                int rows = repairOrderMapper.updateById(order);
                if (rows == 0) {
                    throw new BusinessException("接单失败，工单状态已发生变化（乐观锁拦截）");
                }
        
                sendWebSocketNotify(orderId, newStatus, order.getUserId());
                return;
            } finally {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception ignored) {
                    // Redis 不可用时忽略释放锁
                }
            }
        }

        if (isRepairRole(role) && idEquals(order.getRepairmanId(), currentId)) {
            order.setStatus(newStatus);
            if (newStatus == STATUS_DONE) {
                order.setCompletedTime(LocalDateTime.now());
            }
            int rows = repairOrderMapper.updateById(order);
            if (rows == 0) throw new BusinessException("状态更新失败，请重试（乐观锁拦截）");
            sendWebSocketNotify(orderId, newStatus, order.getUserId());
            return;
        }
        if (isAdminRole(role)) {
            order.setStatus(newStatus);
            if (newStatus == STATUS_DONE) {
                order.setCompletedTime(LocalDateTime.now());
            }
            int rows = repairOrderMapper.updateById(order);
            if (rows == 0) throw new BusinessException("状态更新失败，请重试（乐观锁拦截）");
            sendWebSocketNotify(orderId, newStatus, order.getUserId());
            return;
        }
        throw new BusinessException(403, "无权限操作");
    }

    private void sendWebSocketNotify(Long orderId, int status, Long targetUserId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("status", status);
        payload.put("targetUserId", targetUserId);
        notifyService.sendRepairNotify("STATUS_CHANGED", payload);
    }
    
    @Override
    public void updateStatusWithDetails(Long orderId, Integer newStatus, Long repairmanId, String remark) {
        // 这个方法我们会在后面实现，先保持简单调用updateStatus
        updateStatus(orderId, newStatus);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditOrder(Long orderId, boolean approved, String remark, Boolean assignToPool, Long assignRepairmanId) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!isAdminRole(role)) {
            throw new BusinessException(403, "只有管理员可审核工单");
        }
        
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        
        int oldStatus = order.getStatus();
        if (oldStatus != STATUS_SUBMITTED && oldStatus != STATUS_WAITING_AUDIT) {
            throw new BusinessException(400, "当前状态不允许审核");
        }
        
        order.setRemark(remark);
        order.setAuditTime(LocalDateTime.now());
        
        if (!approved) {
            order.setStatus(STATUS_REJECTED);
            order.setUpdateTime(LocalDateTime.now());
            repairOrderMapper.updateById(order);

            sendWebSocketNotify(orderId, order.getStatus(), order.getUserId());
            return;
        }
        
        if (assignRepairmanId == null) {
            order.setStatus(STATUS_WAITING_DISPATCH);
            order.setRepairmanId(null);
            log.info("审核通过进入待派单: orderId={}", orderId);
        } else {
            SysUser w = validateAssignableRepairman(assignRepairmanId, order.getUserId());
            order.setStatus(STATUS_DISPATCHED);
            order.setRepairmanId(w.getUserId());
            order.setDispatchTime(LocalDateTime.now());
            log.info("审核通过并派单给维修工: orderId={}, repairmanId={}", orderId, w.getUserId());
        }
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);

        sendWebSocketNotify(orderId, order.getStatus(), order.getUserId());
        if (order.getRepairmanId() != null) {
            sendWebSocketNotify(orderId, order.getStatus(), order.getRepairmanId());
        }
    }

    @Override
    public List<DispatchRecommendDTO> recommendRepairmen(Long orderId) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!isAdminRole(role)) {
            throw new BusinessException(403, "只有管理员可查看派单推荐");
        }

        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }

        List<SysUser> workers = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, 1)
                .eq(SysUser::getStatus, 1)
                .ne(order.getUserId() != null, SysUser::getUserId, order.getUserId()));

        List<DispatchRecommendDTO> out = new ArrayList<>();
        for (SysUser worker : workers) {
            Long workerId = worker.getUserId();
            int activeCount = countWorkerOrders(workerId, Arrays.asList(STATUS_DISPATCHED, STATUS_PROCESSING));
            int sameCategoryCount = StringUtils.hasText(order.getCategory())
                    ? countWorkerOrders(workerId, order.getCategory(), null)
                    : 0;
            int sameAreaCount = StringUtils.hasText(order.getArea())
                    ? countWorkerOrders(workerId, null, order.getArea())
                    : 0;
            double avgScore = averageScore(workerId);

            int score = 100;
            score -= activeCount * 20;
            score += Math.min(sameCategoryCount, 5) * 8;
            score += Math.min(sameAreaCount, 5) * 5;
            if (avgScore > 0) {
                score += (int) Math.round(avgScore * 4);
            }
            if (Boolean.TRUE.equals(order.getIsUrgent()) && activeCount == 0) {
                score += 10;
            }
            if (score < 0) {
                score = 0;
            }

            DispatchRecommendDTO dto = new DispatchRecommendDTO();
            dto.setRepairmanId(workerId);
            dto.setUsername(worker.getUsername());
            dto.setRealName(firstName(worker.getRealName(), worker.getNickname(), worker.getUsername()));
            dto.setDepartment(worker.getDepartment());
            dto.setScore(score);
            dto.setActiveCount(activeCount);
            dto.setSameCategoryCount(sameCategoryCount);
            dto.setSameAreaCount(sameAreaCount);
            dto.setAverageScore(avgScore);
            dto.getReasons().add("当前在办工单 " + activeCount + " 单");
            if (sameCategoryCount > 0) {
                dto.getReasons().add("处理过同类故障 " + sameCategoryCount + " 单");
            }
            if (sameAreaCount > 0) {
                dto.getReasons().add("熟悉该区域，历史处理 " + sameAreaCount + " 单");
            }
            if (avgScore > 0) {
                dto.getReasons().add("历史平均评分 " + String.format("%.1f", avgScore) + " 分");
            }
            if (Boolean.TRUE.equals(order.getIsUrgent()) && activeCount == 0) {
                dto.getReasons().add("紧急工单优先推荐空闲维修工");
            }
            out.add(dto);
        }

        out.sort(Comparator.comparing(DispatchRecommendDTO::getScore).reversed()
                .thenComparing(DispatchRecommendDTO::getActiveCount));
        return out;
    }

    private int countWorkerOrders(Long workerId, List<Integer> statuses) {
        LambdaQueryWrapper<RepairOrder> q = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getRepairmanId, workerId);
        if (statuses != null && !statuses.isEmpty()) {
            q.in(RepairOrder::getStatus, statuses);
        }
        Long count = repairOrderMapper.selectCount(q);
        return count == null ? 0 : count.intValue();
    }

    private int countWorkerOrders(Long workerId, String category, String area) {
        LambdaQueryWrapper<RepairOrder> q = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getRepairmanId, workerId)
                .in(RepairOrder::getStatus, Arrays.asList(STATUS_COMPLETED, STATUS_CONFIRMED, STATUS_CLOSED));
        if (StringUtils.hasText(category)) {
            q.eq(RepairOrder::getCategory, category);
        }
        if (StringUtils.hasText(area)) {
            q.eq(RepairOrder::getArea, area);
        }
        Long count = repairOrderMapper.selectCount(q);
        return count == null ? 0 : count.intValue();
    }

    private double averageScore(Long workerId) {
        List<RepairOrder> orders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .select(RepairOrder::getOrderId)
                .eq(RepairOrder::getRepairmanId, workerId)
                .in(RepairOrder::getStatus, Arrays.asList(STATUS_CONFIRMED, STATUS_CLOSED)));
        if (orders == null || orders.isEmpty()) {
            return 0D;
        }
        List<Long> orderIds = new ArrayList<>();
        for (RepairOrder order : orders) {
            if (order.getOrderId() != null) {
                orderIds.add(order.getOrderId());
            }
        }
        if (orderIds.isEmpty()) {
            return 0D;
        }
        List<RepairEvaluation> evaluations = repairEvaluationMapper.selectList(new LambdaQueryWrapper<RepairEvaluation>()
                .in(RepairEvaluation::getOrderId, orderIds));
        if (evaluations == null || evaluations.isEmpty()) {
            return 0D;
        }
        int sum = 0;
        int cnt = 0;
        for (RepairEvaluation evaluation : evaluations) {
            if (evaluation.getScore() != null) {
                sum += evaluation.getScore();
                cnt++;
            }
        }
        if (cnt == 0) {
            return 0D;
        }
        return Math.round((sum * 10.0D / cnt)) / 10.0D;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchOrder(Long orderId, Long repairmanId) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) throw new BusinessException(401, "请先登录");
        if (!isAdminRole(role)) throw new BusinessException(403, "只有管理员可派单");
        
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        
        int oldStatus = order.getStatus();
        if (oldStatus != STATUS_WAITING_DISPATCH) {
            throw new BusinessException(400, "当前状态不允许派单");
        }

        SysUser w = validateAssignableRepairman(repairmanId, order.getUserId());
        order.setRepairmanId(w.getUserId());
        order.setStatus(STATUS_DISPATCHED);
        order.setDispatchTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        

        sendWebSocketNotify(orderId, STATUS_DISPATCHED, order.getUserId());
        sendWebSocketNotify(orderId, STATUS_DISPATCHED, w.getUserId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptOrder(Long orderId) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) throw new BusinessException(401, "请先登录");
        if (!isRepairRole(role)) throw new BusinessException(403, "只有维修工可接单");
        
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        
        int oldStatus = order.getStatus();
        // 维修工仅能接收管理员派给自己的工单；急修仍需管理员派单，保证后勤调度可控。
        boolean canAccept = (oldStatus == STATUS_DISPATCHED);
        if (!canAccept) {
            throw new BusinessException(400, "当前状态不允许接单");
        }
        
        // 已绑定维修工身份时，必须本人。
        if (order.getRepairmanId() != null && !idEquals(order.getRepairmanId(), currentId)) {
            if (oldStatus == STATUS_DISPATCHED) {
                throw new BusinessException(403, "该工单已派给其他维修工");
            }
        }
        
        // 维修工并发接单限制 (<=3单)
        LambdaQueryWrapper<RepairOrder> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(RepairOrder::getRepairmanId, currentId)
                    .eq(RepairOrder::getStatus, STATUS_PROCESSING);
        Long activeCount = repairOrderMapper.selectCount(countWrapper);
        if (activeCount != null && activeCount >= 3) {
            throw new BusinessException(400, "当前处理中工单已达上限(3单)，请先完成手中工单");
        }
        
        // 设置维修工ID和状态
        order.setRepairmanId(currentId);
        order.setStatus(STATUS_PROCESSING);
        order.setStartTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        LambdaQueryWrapper<RepairOrder> casW = new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getOrderId, orderId)
                .eq(RepairOrder::getStatus, oldStatus);
        casW.eq(RepairOrder::getRepairmanId, currentId);
        int rows = repairOrderMapper.update(order, casW);
        if (rows == 0) {
            throw new BusinessException("接单失败，工单已被其他人处理，请刷新后重试");
        }
        

        sendWebSocketNotify(orderId, STATUS_PROCESSING, order.getUserId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long orderId) {
        doCompleteOrderWithImages(orderId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrderWithImages(Long orderId, String completedImages) {
        doCompleteOrderWithImages(orderId, completedImages);
    }

    private void doCompleteOrderWithImages(Long orderId, String completedImages) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) throw new BusinessException(401, "请先登录");
        if (!isRepairRole(role)) throw new BusinessException(403, "只有维修工可完成维修");

        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");

        int oldStatus = order.getStatus();
        if (oldStatus != STATUS_PROCESSING) {
            throw new BusinessException(400, "当前状态不允许完成维修");
        }

        if (!idEquals(order.getRepairmanId(), currentId)) {
            throw new BusinessException(403, "只有负责的维修工可处理该工单");
        }

        order.setStatus(STATUS_COMPLETED);
        order.setCompletedTime(LocalDateTime.now());
        if (StringUtils.hasText(completedImages)) {
            order.setCompletedImages(completedImages);
        }
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);


        sendWebSocketNotify(orderId, STATUS_COMPLETED, order.getUserId());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long orderId) {
        Long currentId = UserContext.getUserId();
        Integer role = UserContext.getRole();
        if (currentId == null) throw new BusinessException(401, "请先登录");
        
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        
        int oldStatus = order.getStatus();
        if (oldStatus != STATUS_COMPLETED) {
            throw new BusinessException(400, "当前状态不允许确认");
        }
        
        if (!idEquals(order.getUserId(), currentId)) {
            throw new BusinessException(403, "只有提交者可确认维修完成");
        }
        
        order.setStatus(STATUS_CONFIRMED);
        order.setConfirmTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order);
        

        // 通知负责维修工：学生已确认（与「谁需感知」一致）
        if (order.getRepairmanId() != null) {
            sendWebSocketNotify(orderId, STATUS_CONFIRMED, order.getRepairmanId());
        }
    }
}
