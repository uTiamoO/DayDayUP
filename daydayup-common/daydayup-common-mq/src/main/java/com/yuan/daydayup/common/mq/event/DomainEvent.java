package com.yuan.daydayup.common.mq.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 领域事件基类
 *
 * <p>领域事件不可变 —— 字段一旦发布就代表"过去发生过的事实"。
 * 子类应通过构造函数提供数据，不开放 setter。</p>
 *
 * @param <T> 事件载荷类型
 */
@Data
public abstract class DomainEvent<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件唯一 ID（用于消费侧幂等） */
    private final String eventId = UUID.randomUUID().toString();

    /** 事件名称：领域:动作，如 game:room.created */
    private final String eventName;

    /** 事件发布时间 */
    private final LocalDateTime occurredAt = LocalDateTime.now();

    /** 事件载荷 */
    private final T payload;

    /** 触发用户 ID（可选） */
    private final Long operatorId;

    protected DomainEvent(String eventName, T payload, Long operatorId) {
        this.eventName = eventName;
        this.payload = payload;
        this.operatorId = operatorId;
    }
}
