package com.campus.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;

    private String content;

    private String images;

    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String senderName;

    @TableField(exist = false)
    private String receiverName;

    @TableField(exist = false)
    private String senderAvatar;
}
