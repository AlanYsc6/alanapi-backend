package com.alan.project.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.alan.project.model.entity.User;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户服务
 *
 * @author alan
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 手机号 + 短信验证码登录（用户不存在时自动注册）
     *
     * @param phone   手机号
     * @param code    短信验证码
     * @param request
     * @return 登录用户信息
     */
    User userLoginByPhone(String phone, String code, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 生成（重新生成）用户的 accessKey / secretKey
     *
     * @param userId 用户 id
     * @return 更新后的用户信息
     */
    User generateKeys(long userId);
}
