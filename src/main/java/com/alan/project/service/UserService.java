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
     * 邮箱 + 验证码登录（用户不存在时自动注册）
     *
     * @param email   邮箱
     * @param code    邮箱验证码
     * @param request
     * @return 登录用户信息
     */
    User userLoginByEmail(String email, String code, HttpServletRequest request);

    /**
     * 通过邮箱验证码重置密码（无需登录）
     *
     * @param email       邮箱
     * @param code        邮箱验证码
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetUserPassword(String email, String code, String newPassword);

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
     * 注销账号：逻辑删除当前登录账号并清除登录态（管理员账号不支持自行注销）
     *
     * @param loginUser 当前登录用户
     * @param request
     * @return 是否注销成功
     */
    boolean userCancel(User loginUser, HttpServletRequest request);

    /**
     * 生成（重新生成）用户的 accessKey / secretKey
     *
     * @param userId 用户 id
     * @return 更新后的用户信息
     */
    User generateKeys(long userId);
}
