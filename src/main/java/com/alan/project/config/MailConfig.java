package com.alan.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮箱 SMTP 配置（application.yml 中 alan.mail 前缀），用于发送邮箱验证码
 *
 * @author alan
 */
@Data
@Component
@ConfigurationProperties(prefix = "alan.mail")
public class MailConfig {

    /**
     * SMTP 服务器地址，如 smtp.qq.com、smtp.163.com
     */
    private String host;

    /**
     * SMTP 端口，SSL 一般为 465
     */
    private Integer port;

    /**
     * 发件邮箱
     */
    private String username;

    /**
     * SMTP 授权码（不是邮箱登录密码）
     */
    private String authCode;
}
