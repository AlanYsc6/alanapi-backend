package com.alan.project.manager;

import com.alan.project.common.ErrorCode;
import com.alan.project.config.SmsConfig;
import com.alan.project.exception.BusinessException;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponseBody;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponseBody;
import com.aliyun.teaopenapi.models.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 阿里云短信认证管理器：验证码由阿里云生成、存储并校验（SendSmsVerifyCode / CheckSmsVerifyCode），
 * 计费走"融合认证解决方案-通信服务套餐包"
 *
 * @author alan
 */
@Slf4j
@Component
public class SmsManager {

    /**
     * 中国大陆手机号
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 发送频率限制的 Redis Key 前缀
     */
    private static final String SMS_LIMIT_KEY = "alan:sms:limit:";

    @Resource
    private SmsConfig smsConfig;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private Client client;

    @PostConstruct
    public void init() throws Exception {
        if (StringUtils.isAnyBlank(smsConfig.getAccessKeyId(), smsConfig.getAccessKeySecret())) {
            log.warn("短信服务未配置 AccessKey，发送短信验证码功能不可用");
            return;
        }
        Config config = new Config()
                .setAccessKeyId(smsConfig.getAccessKeyId())
                .setAccessKeySecret(smsConfig.getAccessKeySecret())
                .setEndpoint("dypnsapi.aliyuncs.com");
        client = new Client(config);
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     */
    public void sendSmsVerifyCode(String phone) {
        if (!isValidPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号不合法");
        }
        if (client == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信服务未配置，请联系管理员");
        }
        // 同一手机号 60 秒内只允许发一条，保护套餐包余量；发送失败时删掉限流键允许立即重试
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(SMS_LIMIT_KEY + phone, "1", Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(first)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送太频繁，请 1 分钟后再试");
        }
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setSignName(smsConfig.getSignName())
                .setTemplateCode(smsConfig.getTemplateCode())
                // 模板变量：验证码位置用 ##code## 占位，由阿里云生成并支持 CheckSmsVerifyCode 校验；
                // min 对应模板里的 ${min}（有效期分钟数），与下面 validTime=300 秒保持一致
                .setTemplateParam("{\"code\":\"##code##\",\"min\":\"5\"}")
                .setCodeType(1L)
                .setCodeLength(6L)
                .setValidTime(300L);
        SendSmsVerifyCodeResponseBody body;
        try {
            body = client.sendSmsVerifyCode(request).getBody();
        } catch (Exception e) {
            stringRedisTemplate.delete(SMS_LIMIT_KEY + phone);
            log.error("发送短信验证码异常, phone={}", phone, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }
        if (body == null || !"OK".equals(body.getCode())) {
            stringRedisTemplate.delete(SMS_LIMIT_KEY + phone);
            String message = body == null ? "无响应" : body.getMessage();
            log.error("发送短信验证码失败, phone={}, message={}", phone, message);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码发送失败：" + message);
        }
    }

    /**
     * 校验用户输入的验证码
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true 表示校验通过
     */
    public boolean checkSmsVerifyCode(String phone, String code) {
        if (!isValidPhone(phone) || StringUtils.isBlank(code)) {
            return false;
        }
        if (client == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信服务未配置，请联系管理员");
        }
        CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setVerifyCode(code);
        CheckSmsVerifyCodeResponseBody body;
        try {
            body = client.checkSmsVerifyCode(request).getBody();
        } catch (Exception e) {
            log.error("校验短信验证码异常, phone={}", phone, e);
            return false;
        }
        // verifyResult: PASS-校验通过，REJECT-校验不通过，UNKNOWN-无法判断
        return body != null && body.getModel() != null
                && "PASS".equals(body.getModel().getVerifyResult());
    }

    /**
     * 校验手机号格式
     */
    public boolean isValidPhone(String phone) {
        return StringUtils.isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }
}
