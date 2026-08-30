package com.alan.project.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 通过邮箱验证码重置密码请求
 *
 * @author alan
 */
@Data
public class UserResetPasswordRequest implements Serializable {

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String code;

    /**
     * 新密码（至少 8 位）
     */
    private String newPassword;

    private static final long serialVersionUID = 1L;
}
