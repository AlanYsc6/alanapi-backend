package com.alan.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山引擎 TOS 对象存储配置（application.yml 中 tos 前缀）
 *
 * @author alan
 */
@Data
@Component
@ConfigurationProperties(prefix = "tos")
public class TosConfig {

    /**
     * 接入点，如 tos-cn-beijing.volces.com
     */
    private String endpoint;

    /**
     * 区域，如 cn-beijing
     */
    private String region;

    /**
     * 访问密钥 AK
     */
    private String accessKey;

    /**
     * 秘密密钥 SK
     */
    private String secretKey;

    /**
     * 桶名
     */
    private String bucketName;

    /**
     * 自定义访问域名（绑定了 CDN/自定义域名时填写，末尾不带斜杠），为空则用桶默认域名
     */
    private String domain;
}
