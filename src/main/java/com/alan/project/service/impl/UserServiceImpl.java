package com.alan.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.alan.project.common.ErrorCode;
import com.alan.project.exception.BusinessException;
import com.alan.project.manager.MailManager;
import com.alan.project.manager.SmsManager;
import com.alan.project.mapper.UserMapper;
import com.alan.project.model.entity.User;
import com.alan.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static com.alan.project.constant.UserConstant.ADMIN_ROLE;
import static com.alan.project.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 用户服务实现类
 *
 * @author alan
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private SmsManager smsManager;

    @Resource
    private MailManager mailManager;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "alan";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 冻结账号禁止登录
        checkUserFrozen(user);
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return user;
    }

    /**
     * 手机号 + 短信验证码登录（用户不存在时自动注册）
     *
     * @param phone   手机号
     * @param code    短信验证码
     * @param request
     * @return
     */
    @Override
    public User userLoginByPhone(String phone, String code, HttpServletRequest request) {
        // 1. 校验参数
        if (!smsManager.isValidPhone(phone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号不合法");
        }
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        // 2. 到阿里云校验验证码（验证码由阿里云生成和存储）
        if (!smsManager.checkSmsVerifyCode(phone, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        // 3. 查询用户，不存在则自动注册（手机号即账号）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setUserAccount(phone);
            // 随机密码占位，短信登录方式用不到密码
            user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + UUID.randomUUID()).getBytes()));
            user.setUserName("用户" + StringUtils.right(phone, 4));
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
        }
        // 冻结账号禁止登录
        checkUserFrozen(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return user;
    }

    /**
     * 邮箱 + 验证码登录（用户不存在时自动注册）
     *
     * @param email   邮箱
     * @param code    邮箱验证码
     * @param request
     * @return
     */
    @Override
    public User userLoginByEmail(String email, String code, HttpServletRequest request) {
        // 1. 校验参数
        if (!mailManager.isValidEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        // 2. 校验验证码（后端生成存 Redis，校验通过后立即删除）
        if (!mailManager.checkMailCode(email, MailManager.TYPE_LOGIN, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        // 3. 查询用户，不存在则自动注册（邮箱即账号）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUserAccount(email);
            // 随机密码占位，邮箱验证码登录方式用不到密码
            user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + UUID.randomUUID()).getBytes()));
            user.setUserName(StringUtils.substringBefore(email, "@"));
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
        }
        // 冻结账号禁止登录
        checkUserFrozen(user);
        // 4. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return user;
    }

    /**
     * 通过邮箱验证码重置密码（无需登录）
     *
     * @param email       邮箱
     * @param code        邮箱验证码
     * @param newPassword 新密码
     * @return
     */
    @Override
    public boolean resetUserPassword(String email, String code, String newPassword) {
        // 1. 校验参数
        if (!mailManager.isValidEmail(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        if (StringUtils.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能少于 8 位");
        }
        // 2. 校验验证码
        if (!mailManager.checkMailCode(email, MailManager.TYPE_RESET, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        // 3. 邮箱必须已绑定账号
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该邮箱未绑定账号");
        }
        // 4. 更新密码
        user.setUserPassword(DigestUtils.md5DigestAsHex((SALT + newPassword).getBytes()));
        return this.updateById(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 冻结账号的存量会话在各请求鉴权时直接拦截
        checkUserFrozen(currentUser);
        return currentUser;
    }

    /**
     * 账号冻结校验：冻结用户禁止登录，已登录的存量会话也会被拦截
     */
    private void checkUserFrozen(User user) {
        if (user != null && user.getUserStatus() != null && user.getUserStatus() == 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该账号已被冻结，请联系管理员");
        }
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 注销账号：逻辑删除当前登录账号并清除登录态（管理员账号不支持自行注销）
     */
    @Override
    public boolean userCancel(User loginUser, HttpServletRequest request) {
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "管理员账号不支持自行注销");
        }
        boolean result = this.removeById(loginUser.getId());
        if (result) {
            // 注销后立即清除登录态，本次会话不再可用
            request.getSession().removeAttribute(USER_LOGIN_STATE);
        }
        return result;
    }

    /**
     * 生成（重新生成）用户的 accessKey / secretKey
     *
     * @param userId 用户 id
     * @return
     */
    @Override
    public User generateKeys(long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        User updateUser = new User();
        updateUser.setId(userId);
        // accessKey 相当于账号标识，短一点即可；secretKey 是签名密钥，用 32 位 UUID 保证随机性
        updateUser.setAccessKey(RandomUtil.randomString(10));
        updateUser.setSecretKey(IdUtil.fastSimpleUUID());
        boolean result = this.updateById(updateUser);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成密钥失败，数据库错误");
        }
        return this.getById(userId);
    }

}




