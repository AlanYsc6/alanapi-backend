package com.alan.project.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 手机号 + 短信验证码登录请求
 *
 * @author alan
 */
@Data
public class UserLoginByPhoneRequest implements Serializable {

    /**
     * 手机号
     */
    private String phone;

    /**
     * 短信验证码
     */
    private String code;

    private static final long serialVersionUID = 1L;
}
