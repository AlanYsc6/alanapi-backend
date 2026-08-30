package com.alan.project.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 邮箱 + 验证码登录请求
 *
 * @author alan
 */
@Data
public class UserEmailLoginRequest implements Serializable {

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String code;

    private static final long serialVersionUID = 1L;
}
