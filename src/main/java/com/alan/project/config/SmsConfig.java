package com.alan.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云号码认证服务（融合认证-短信认证）配置（application.yml 中 alan.sms 前缀）
 *
 * @author alan
 */
@Data
@Component
@ConfigurationProperties(prefix = "alan.sms")
public class SmsConfig {

    /**
     * 阿里云 AccessKey ID（建议使用仅授权 dypnsapi 的 RAM 用户）
     */
    private String accessKeyId;

    /**
     * 阿里云 AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 短信签名名称（控制台赠送的签名）
     */
    private String signName;

    /**
     * 短信模板 Code（控制台赠送的模板，与签名配套使用）
     */
    private String templateCode;
}
