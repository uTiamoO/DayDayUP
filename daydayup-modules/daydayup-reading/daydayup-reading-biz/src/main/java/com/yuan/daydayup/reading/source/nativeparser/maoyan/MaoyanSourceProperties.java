package com.yuan.daydayup.reading.source.nativeparser.maoyan;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 猫眼看书运行时 Header 配置（{@code reading.source.maoyan.*}）。
 *
 * <p>Authorization 与设备标识默认留空，只允许通过部署环境注入，不从书源 JSON 回填。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "reading.source.maoyan")
public class MaoyanSourceProperties {

    private String authorization = "";
    private String clientDevice = "";
    private String clientBrand = "";
    private String clientVersion = "2.3.0";
    private String clientChannel = "android";
    private String clientName = "app.maoyankanshu.novel";
    private String clientSource = "android";
    private String aliasName = "maoyankanshu";
}
