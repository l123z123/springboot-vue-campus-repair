package com.campus.repair.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.repair.common.BusinessException;
import com.campus.repair.context.UserContext;
import com.campus.repair.entity.ChatMessage;
import com.campus.repair.entity.RepairOrder;
import com.campus.repair.entity.SysUser;
import com.campus.repair.mapper.ChatMessageMapper;
import com.campus.repair.mapper.RepairOrderMapper;
import com.campus.repair.mapper.SysUserMapper;
import com.campus.repair.service.ChatMessageService;
import com.campus.repair.service.NotifyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageMapper chatMessageMapper;
    private final SysUserMapper sysUserMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final NotifyService notifyService;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper, SysUserMapper sysUserMapper,
            RepairOrderMapper repairOrderMapper, NotifyService notifyService) {
        this.chatMessageMapper = chatMessageMapper;
        this.sysUserMapper = sysUserMapper;
        this.repairOrderMapper = repairOrderMapper;
        this.notifyService = notifyService;
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

    @Override
    public ChatMessage sendMessage(Long orderId, Long receiverId, String content, String images) {
        Long senderId = UserContext.getUserId();
        if (senderId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!StringUtils.hasText(content) && !StringUtils.hasText(images)) {
            throw new BusinessException("消息内容不能为空");
        }
        if (orderId == null) {
            throw new BusinessException("缺少工单ID");
        }

        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        assertChatParticipant(order, senderId);

        if (order.getRepairmanId() == null) {
            throw new BusinessException(400, "需维修工已分配后才能发起沟通");
        }
        if (order.getUserId() == null) {
            throw new BusinessException(400, "工单数据异常");
        }

        Long to = receiverId;
        if (to == null) {
            to = idEquals(order.getUserId(), senderId) ? order.getRepairmanId() : order.getUserId();
        }
        if (to == null) {
            throw new BusinessException(400, "无法确定接收方，请重试");
        }
        if (!idEquals(to, order.getUserId()) && !idEquals(to, order.getRepairmanId())) {
            throw new BusinessException(400, "接收人必须是本工单的报修学生或负责维修工");
        }
        if (idEquals(to, senderId)) {
            throw new BusinessException(400, "不能给自己发消息");
        }

        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId);
        message.setSenderId(senderId);
        message.setReceiverId(to);
        message.setContent(StringUtils.hasText(content) ? content : "");
        message.setImages(StringUtils.hasText(images) ? images : null);
        message.setIsRead(0);

        int rows = chatMessageMapper.insert(message);
        if (rows <= 0) {
            throw new BusinessException("发送消息失败");
        }

        ChatMessage result = chatMessageMapper.selectById(message.getId());
        enrichMessage(result);

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", String.valueOf(orderId));
        payload.put("targetUserId", to);
        String preview = StringUtils.hasText(content) ? (content.length() > 60 ? content.substring(0, 60) + "…" : content) : "[图片]";
        payload.put("preview", preview);
        if (result != null && result.getSenderName() != null) {
            payload.put("senderName", result.getSenderName());
        } else {
            SysUser s = sysUserMapper.selectById(senderId);
            if (s != null) {
                String name = s.getRealName() != null && !s.getRealName().isEmpty() ? s.getRealName() : s.getUsername();
                payload.put("senderName", name);
            }
        }
        try {
            notifyService.sendRepairNotify("CHAT", payload);
        } catch (Exception ignored) {
            // 推送失败不影响发消息
        }
        return result;
    }

    @Override
    public IPage<ChatMessage> getMessageList(Page<ChatMessage> page, Long orderId) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        RepairOrder order = repairOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        assertChatViewPermission(order, userId);

        LambdaQueryWrapper<ChatMessage> q = new LambdaQueryWrapper<>();
        q.eq(ChatMessage::getOrderId, orderId);
        q.orderByAsc(ChatMessage::getCreateTime);

        IPage<ChatMessage> result = chatMessageMapper.selectPage(page, q);
        result.getRecords().forEach(this::enrichMessage);
        if (userId != null) {
            markAsRead(orderId, userId);
        }
        return result;
    }

    private void assertChatParticipant(RepairOrder order, Long userId) {
        Integer role = UserContext.getRole() != null ? UserContext.getRole() : 0;
        // 管理员如果是工单报修人（自己提交的报修），允许参与聊天
        if (role.intValue() == 2 && !idEquals(order.getUserId(), userId)) {
            throw new BusinessException(403, "管理员仅可参与自己提交的工单沟通");
        }
        if (idEquals(order.getUserId(), userId)) {
            return;
        }
        if (idEquals(order.getRepairmanId(), userId)) {
            return;
        }
        throw new BusinessException(403, "您不是本工单的沟通方");
    }

    private void assertChatViewPermission(RepairOrder order, Long userId) {
        Integer role = UserContext.getRole() != null ? UserContext.getRole() : 0;
        if (role.intValue() == 2) {
            return;
        }
        assertChatParticipant(order, userId);
    }

    @Override
    public List<ChatMessage> getUnreadMessages(Long userId) {
        LambdaQueryWrapper<ChatMessage> q = new LambdaQueryWrapper<>();
        q.eq(ChatMessage::getReceiverId, userId);
        q.eq(ChatMessage::getIsRead, 0);
        q.orderByDesc(ChatMessage::getCreateTime);

        List<ChatMessage> messages = chatMessageMapper.selectList(q);
        messages.forEach(this::enrichMessage);
        return messages;
    }

    @Override
    public void markAsRead(Long orderId, Long userId) {
        if (userId == null) {
            return;
        }
        LambdaQueryWrapper<ChatMessage> q = new LambdaQueryWrapper<>();
        q.eq(ChatMessage::getOrderId, orderId);
        q.eq(ChatMessage::getReceiverId, userId);
        q.eq(ChatMessage::getIsRead, 0);

        ChatMessage updateEntity = new ChatMessage();
        updateEntity.setIsRead(1);
        chatMessageMapper.update(updateEntity, q);
    }

    @Override
    public IPage<ChatMessage> getRecentConversations(Page<ChatMessage> page, Long userId) {
        List<ChatMessage> all = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .and(w -> w.eq(ChatMessage::getSenderId, userId).or().eq(ChatMessage::getReceiverId, userId))
                        .orderByDesc(ChatMessage::getCreateTime));
        if (all.isEmpty()) {
            Page<ChatMessage> empty = new Page<>(page.getCurrent(), page.getSize());
            empty.setTotal(0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        Map<Long, ChatMessage> best = new java.util.LinkedHashMap<>();
        for (ChatMessage m : all) {
            Long oid = m.getOrderId();
            if (oid != null && !best.containsKey(oid)) {
                best.put(oid, m);
            }
        }
        List<ChatMessage> unique = best.values().stream()
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .collect(Collectors.toList());
        for (ChatMessage m : unique) {
            enrichMessage(m);
        }
        int total = unique.size();
        int start = (int) ((page.getCurrent() - 1) * page.getSize());
        int end = Math.min(start + (int) page.getSize(), total);
        Page<ChatMessage> out = new Page<>(page.getCurrent(), page.getSize());
        out.setTotal(total);
        if (start < total) {
            out.setRecords(unique.subList(start, end));
        } else {
            out.setRecords(Collections.emptyList());
        }
        return out;
    }

    private void enrichMessage(ChatMessage message) {
        if (message == null) {
            return;
        }

        if (message.getSenderId() != null) {
            SysUser sender = sysUserMapper.selectById(message.getSenderId());
            if (sender != null) {
                if (StringUtils.hasText(sender.getRealName())) {
                    message.setSenderName(sender.getRealName());
                } else if (StringUtils.hasText(sender.getNickname())) {
                    message.setSenderName(sender.getNickname());
                } else {
                    message.setSenderName(sender.getUsername());
                }
                message.setSenderAvatar(sender.getAvatar());
            }
        }

        if (message.getReceiverId() != null) {
            SysUser receiver = sysUserMapper.selectById(message.getReceiverId());
            if (receiver != null) {
                if (StringUtils.hasText(receiver.getRealName())) {
                    message.setReceiverName(receiver.getRealName());
                } else if (StringUtils.hasText(receiver.getNickname())) {
                    message.setReceiverName(receiver.getNickname());
                } else {
                    message.setReceiverName(receiver.getUsername());
                }
            }
        }
    }
}
