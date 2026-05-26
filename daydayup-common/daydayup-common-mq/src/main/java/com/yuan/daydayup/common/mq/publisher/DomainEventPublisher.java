package com.yuan.daydayup.common.mq.publisher;

import com.yuan.daydayup.common.core.context.UserContextHolder;
import com.yuan.daydayup.common.mq.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * 领域事件发布器
 *
 * <p>规则：</p>
 * <ul>
 *   <li>topic = 事件名称（{@link DomainEvent#getEventName()}）</li>
 *   <li>消息 key = eventId，可用于消费侧幂等</li>
 *   <li>发送失败仅记录日志，由调用方决定是否使用事务消息保证一致性</li>
 * </ul>
 *
 * <p>由 {@code MqAutoConfiguration} 装配，无需业务侧手动声明 Bean。</p>
 */
@Slf4j
public class DomainEventPublisher {

    private final RocketMQTemplate rocketMQTemplate;

    public DomainEventPublisher(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public <T> void publish(DomainEvent<T> event) {
        Message<DomainEvent<T>> message = MessageBuilder.withPayload(event)
                .setHeader("KEYS", event.getEventId())
                .build();
        try {
            rocketMQTemplate.send(event.getEventName(), message);
        } catch (RuntimeException ex) {
            log.error("领域事件发布失败：eventName={}, eventId={}",
                    event.getEventName(), event.getEventId(), ex);
            throw ex;
        }
    }

    /**
     * 事务消息：与业务数据库操作绑定到同一事务
     *
     * @param event   领域事件
     * @param arg     传递给本地事务监听器的参数
     */
    public <T> void publishInTransaction(DomainEvent<T> event, Object arg) {
        Message<DomainEvent<T>> message = MessageBuilder.withPayload(event)
                .setHeader("KEYS", event.getEventId())
                .build();
        rocketMQTemplate.sendMessageInTransaction(event.getEventName(), message, arg);
    }

    /**
     * 便捷方法：自动从 {@link UserContextHolder} 补全 operatorId 后发布
     */
    public <T> void publishWithContext(java.util.function.Function<Long, DomainEvent<T>> factory) {
        Long operatorId = UserContextHolder.getUserId();
        publish(factory.apply(operatorId));
    }
}
