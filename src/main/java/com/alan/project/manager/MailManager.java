package com.alan.project.manager;

import cn.hutool.core.util.RandomUtil;
import com.alan.project.common.ErrorCode;
import com.alan.project.config.MailConfig;
import com.alan.project.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 邮箱验证码管理器：验证码后端生成，存 Redis（5 分钟过期），用于邮箱登录和重置密码。
 * 未配置 SMTP 时为开发模式，验证码只打印到日志，方便本地跑通流程。
 *
 * @author alan
 */
@Slf4j
@Component
public class MailManager {

    /**
     * 邮箱格式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * 验证码类型：邮箱登录
     */
    public static final String TYPE_LOGIN = "login";

    /**
     * 验证码类型：重置密码
     */
    public static final String TYPE_RESET = "reset";

    private static final String MAIL_CODE_KEY = "alan:mail:code:";

    private static final String MAIL_LIMIT_KEY = "alan:mail:limit:";

    @Resource
    private MailConfig mailConfig;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private JavaMailSenderImpl mailSender;

    @PostConstruct
    public void init() {
        if (StringUtils.isAnyBlank(mailConfig.getHost(), mailConfig.getUsername(), mailConfig.getAuthCode())) {
            log.warn("邮箱 SMTP 未配置，验证码邮件不会真正发送（开发模式，验证码打印到日志）");
            return;
        }
        mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailConfig.getHost());
        mailSender.setPort(mailConfig.getPort() != null ? mailConfig.getPort() : 465);
        mailSender.setUsername(mailConfig.getUsername());
        mailSender.setPassword(mailConfig.getAuthCode());
        mailSender.setDefaultEncoding("UTF-8");
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        // 465 走 SSL，其他端口（如 587）走 STARTTLS
        if (mailSender.getPort() == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
    }

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱
     * @param type  验证码类型：login（邮箱登录）/ reset（重置密码）
     */
    public void sendMailCode(String email, String type) {
        if (!isValidEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        if (!TYPE_LOGIN.equals(type) && !TYPE_RESET.equals(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码类型不合法");
        }
        // 同一邮箱同一类型 60 秒内只允许发一次
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(MAIL_LIMIT_KEY + type + ":" + email, "1", Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送太频繁，请 1 分钟后再试");
        }
        String code = RandomUtil.randomNumbers(6);
        // 验证码存 Redis，5 分钟过期
        stringRedisTemplate.opsForValue().set(MAIL_CODE_KEY + type + ":" + email, code, Duration.ofMinutes(5));
        // 开发模式：未配置 SMTP 时只打印验证码到日志
        if (mailSender == null) {
            log.info("[开发模式] 邮箱验证码 email={}, type={}, code={}", email, type, code);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailConfig.getUsername());
            message.setTo(email);
            message.setSubject("alan接口 验证码");
            message.setText("您的验证码是 " + code + "，5 分钟内有效。若非本人操作，请忽略本邮件。");
            mailSender.send(message);
        } catch (Exception e) {
            stringRedisTemplate.delete(MAIL_LIMIT_KEY + type + ":" + email);
            stringRedisTemplate.delete(MAIL_CODE_KEY + type + ":" + email);
            log.error("发送邮箱验证码异常, email={}, type={}", email, type, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码邮件发送失败，请稍后重试");
        }
    }

    /**
     * 校验用户输入的邮箱验证码，校验通过后立即删除
     *
     * @param email 邮箱
     * @param type  验证码类型
     * @param code  用户输入的验证码
     * @return true 表示校验通过
     */
    public boolean checkMailCode(String email, String type, String code) {
        if (!isValidEmail(email) || StringUtils.isBlank(code)) {
            return false;
        }
        String key = MAIL_CODE_KEY + type + ":" + email;
        String saved = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(saved)) {
            return false;
        }
        boolean ok = saved.equals(code.trim());
        if (ok) {
            stringRedisTemplate.delete(key);
        }
        return ok;
    }

    /**
     * 校验邮箱格式
     */
    public boolean isValidEmail(String email) {
        return StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }
}
