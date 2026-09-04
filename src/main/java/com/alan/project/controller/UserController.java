package com.alan.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.alan.project.annotation.AuthCheck;
import com.alan.project.common.BaseResponse;
import com.alan.project.common.DeleteRequest;
import com.alan.project.common.ErrorCode;
import com.alan.project.common.ResultUtils;
import com.alan.project.constant.UserConstant;
import com.alan.project.exception.BusinessException;
import com.alan.project.model.dto.*;
import com.alan.project.model.dto.user.*;
import com.alan.project.model.entity.User;
import com.alan.project.model.entity.UserInterfaceInfo;
import com.alan.project.manager.MailManager;
import com.alan.project.manager.SmsManager;
import com.alan.project.model.vo.UserVO;
import com.alan.project.service.UserService;
import com.alan.project.service.UserInterfaceInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author alan
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Resource
    private SmsManager smsManager;

    @Resource
    private MailManager mailManager;

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 发送手机号登录短信验证码
     *
     * @param phone 手机号
     * @return
     */
    @GetMapping("/sms/send")
    public BaseResponse<Boolean> sendSmsCode(String phone) {
        smsManager.sendSmsVerifyCode(phone);
        return ResultUtils.success(true);
    }

    /**
     * 手机号 + 短信验证码登录（用户不存在时自动注册）
     *
     * @param userLoginByPhoneRequest
     * @param request
     * @return
     */
    @PostMapping("/login/phone")
    public BaseResponse<User> userLoginByPhone(@RequestBody UserLoginByPhoneRequest userLoginByPhoneRequest,
                                               HttpServletRequest request) {
        if (userLoginByPhoneRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLoginByPhone(userLoginByPhoneRequest.getPhone(),
                userLoginByPhoneRequest.getCode(), request);
        return ResultUtils.success(user);
    }

    /**
     * 发送邮箱验证码
     *
     * @param email 邮箱
     * @param type  验证码类型：login（邮箱登录）/ reset（重置密码）
     * @return
     */
    @GetMapping("/mail/send")
    public BaseResponse<Boolean> sendMailCode(String email, String type) {
        // 重置密码场景要求邮箱已绑定账号，提前拦截避免泄露信息错误
        if ("reset".equals(type)) {
            if (!mailManager.isValidEmail(email)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
            }
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", email);
            long count = userService.count(queryWrapper);
            if (count <= 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "该邮箱未绑定账号");
            }
        }
        mailManager.sendMailCode(email, type);
        return ResultUtils.success(true);
    }

    /**
     * 邮箱 + 验证码登录（用户不存在时自动注册）
     *
     * @param userEmailLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login/email")
    public BaseResponse<User> userLoginByEmail(@RequestBody UserEmailLoginRequest userEmailLoginRequest,
                                               HttpServletRequest request) {
        if (userEmailLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLoginByEmail(userEmailLoginRequest.getEmail(),
                userEmailLoginRequest.getCode(), request);
        return ResultUtils.success(user);
    }

    /**
     * 通过邮箱验证码重置密码（无需登录）
     *
     * @param userResetPasswordRequest
     * @return
     */
    @PostMapping("/reset/password")
    public BaseResponse<Boolean> resetUserPassword(@RequestBody UserResetPasswordRequest userResetPasswordRequest) {
        if (userResetPasswordRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.resetUserPassword(userResetPasswordRequest.getEmail(),
                userResetPasswordRequest.getCode(), userResetPasswordRequest.getNewPassword());
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    // endregion

    /**
     * 生成（重新生成）当前登录用户的 accessKey / secretKey
     *
     * @param request
     * @return 带有最新密钥的用户信息
     */
    @PostMapping("/generateKey")
    public BaseResponse<UserVO> generateKey(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        User updatedUser = userService.generateKeys(loginUser.getId());
        // 同步刷新 session 中的用户信息
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, updatedUser);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(updatedUser, userVO);
        return ResultUtils.success(userVO);
    }

    /**
     * 注销账号（用户自行注销，逻辑删除当前账号并退出登录；管理员账号不支持）
     *
     * @param request
     * @return
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> userCancel(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.userCancel(loginUser, request);
        return ResultUtils.success(result);
    }

    /**
     * 更新个人信息（当前登录用户，只能改昵称、头像、性别）
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userUpdateMyRequest.getUserName() != null && userUpdateMyRequest.getUserName().length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称过长");
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        // 强制使用登录用户的 id，防止改到别人
        user.setId(loginUser.getId());
        boolean result = userService.updateById(user);
        if (result) {
            // 同步刷新 session 中的用户信息，否则全局状态（头像/昵称/水印）不会更新
            User updatedUser = userService.getById(loginUser.getId());
            request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, updatedUser);
        }
        return ResultUtils.success(result);
    }

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        boolean result = userService.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer userStatus = userUpdateRequest.getUserStatus();
        if (userStatus != null && !Objects.equals(userStatus, 0) && !Objects.equals(userStatus, 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号状态不合法");
        }
        // 管理员不能冻结自己，避免把自己锁在管理页外面
        User loginUser = userService.getLoginUser(request);
        if (Objects.equals(userStatus, 1) && loginUser.getId().equals(userUpdateRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能冻结当前登录账号");
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取用户
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<UserVO> getUserById(int id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<UserVO>> listUser(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        List<User> userList = userService.list(queryWrapper);
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页获取用户列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<UserVO>> listUserByPage(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        long current = 1;
        long size = 10;
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
            current = userQueryRequest.getCurrent();
            size = userQueryRequest.getPageSize();
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        Page<User> userPage = userService.page(new Page<>(current, size), queryWrapper);
        Page<UserVO> userVOPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        // 补充每个用户的剩余可调用次数（名下全部已开通接口之和）
        fillLeftNum(userVOList);
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 批量填充用户剩余可调用次数
     */
    private void fillLeftNum(List<UserVO> userVOList) {
        if (CollectionUtils.isEmpty(userVOList)) {
            return;
        }
        Set<Long> userIds = userVOList.stream()
                .map(UserVO::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<UserInterfaceInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserInterfaceInfo::getUserId, userIds);
        Map<Long, Integer> leftNumMap = userInterfaceInfoService.list(queryWrapper).stream()
                .collect(Collectors.groupingBy(UserInterfaceInfo::getUserId,
                        Collectors.summingInt(info -> info.getLeftNum() == null ? 0 : info.getLeftNum())));
        userVOList.forEach(userVO -> userVO.setLeftNum(
                leftNumMap.getOrDefault(userVO.getId(), 0).longValue()));
    }

    // endregion
}
