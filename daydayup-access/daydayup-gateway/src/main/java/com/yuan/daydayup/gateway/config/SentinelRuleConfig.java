package com.yuan.daydayup.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关限流规则配置
 *
 * <p>启动时加载默认限流规则，骨架开箱即用（无需依赖 Sentinel Dashboard 或 Nacos 推送）。
 * 后续可通过 Dashboard 实时调整规则，或接入 Nacos 数据源实现动态配置。</p>
 *
 * <p>当前默认规则：</p>
 * <ul>
 *   <li>daydayup-auth：登录接口限流 10 QPS / IP，防止暴力破解</li>
 *   <li>daydayup-admin：后台管理接口限流 50 QPS，防止单服务过载</li>
 *   <li>daydayup-game / daydayup-social：业务接口限流 100 QPS</li>
 * </ul>
 *
 * <p>限流触发后由 {@link com.yuan.daydayup.gateway.handler.GatewaySentinelHandler}
 * 返回 HTTP 503 + JSON 错误响应。</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class SentinelRuleConfig {

    @PostConstruct
    public void init() {
        loadGatewayRules();
        log.info("Sentinel 网关默认限流规则已加载");
    }

    private void loadGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 登录接口限流：每个客户端 IP 10 QPS，防止单 IP 暴力破解
        // 注：client IP 取 remoteAddress；生产前置可信代理时需配合网关 XFF 解析策略
        GatewayFlowRule authRule = new GatewayFlowRule("daydayup-auth")
                .setCount(10)
                .setIntervalSec(1)
                .setParamItem(new GatewayParamFlowItem()
                        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP));
        rules.add(authRule);

        // 后台管理接口：50 QPS
        GatewayFlowRule adminRule = new GatewayFlowRule("daydayup-admin")
                .setCount(50)
                .setIntervalSec(1);
        rules.add(adminRule);

        // 业务接口：100 QPS
        GatewayFlowRule gameRule = new GatewayFlowRule("daydayup-game")
                .setCount(100)
                .setIntervalSec(1);
        rules.add(gameRule);

        GatewayFlowRule socialRule = new GatewayFlowRule("daydayup-social")
                .setCount(100)
                .setIntervalSec(1);
        rules.add(socialRule);

        GatewayRuleManager.loadRules(rules);
    }
}
