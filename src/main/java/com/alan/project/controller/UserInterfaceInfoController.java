package com.alan.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.alan.project.annotation.AuthCheck;
import com.alan.project.common.BaseResponse;
import com.alan.project.common.DeleteRequest;
import com.alan.project.common.ErrorCode;
import com.alan.project.common.ResultUtils;
import com.alan.project.exception.BusinessException;
import com.alan.project.model.dto.userinterfaceinfo.UserInterfaceInfoAddRequest;
import com.alan.project.model.dto.userinterfaceinfo.UserInterfaceInfoChargeRequest;
import com.alan.project.model.dto.userinterfaceinfo.UserInterfaceInfoInvokeRequest;
import com.alan.project.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.alan.project.model.dto.userinterfaceinfo.UserInterfaceInfoUpdateRequest;
import com.alan.project.model.entity.InterfaceInfo;
import com.alan.project.model.entity.User;
import com.alan.project.model.entity.UserInterfaceInfo;
import com.alan.project.service.InterfaceInfoService;
import com.alan.project.service.UserService;
import com.alan.project.service.UserInterfaceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户调用接口关系接口（调用次数分配与计数管理，仅管理员可操作）
 *
 * @author alan
 */
@RestController
@RequestMapping("/userInterfaceInfo")
@Slf4j
public class UserInterfaceInfoController {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Resource
    private UserService userService;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    /**
     * 为用户分配接口调用次数（首次开通）
     *
     * @param userInterfaceInfoAddRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/add")
    public BaseResponse<Long> addUserInterfaceInfo(@RequestBody UserInterfaceInfoAddRequest userInterfaceInfoAddRequest) {
        if (userInterfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
        BeanUtils.copyProperties(userInterfaceInfoAddRequest, userInterfaceInfo);
        userInterfaceInfo.setStatus(0);
        userInterfaceInfo.setTotalNum(0);
        // 校验
        userInterfaceInfoService.validUserInterfaceInfo(userInterfaceInfo, true);
        // 用户与接口必须真实存在
        if (userService.getById(userInterfaceInfo.getUserId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "调用用户不存在");
        }
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(userInterfaceInfo.getInterfaceInfoId());
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "接口不存在");
        }
        // 同一用户对同一接口仅允许一条调用关系
        LambdaQueryWrapper<UserInterfaceInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInterfaceInfo::getUserId, userInterfaceInfo.getUserId());
        queryWrapper.eq(UserInterfaceInfo::getInterfaceInfoId, userInterfaceInfo.getInterfaceInfoId());
        if (userInterfaceInfoService.count(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已开通此接口的调用权限，请直接更新剩余次数");
        }
        boolean result = userInterfaceInfoService.save(userInterfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(userInterfaceInfo.getId());
    }

    /**
     * 删除调用关系
     *
     * @param deleteRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserInterfaceInfo(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        if (userInterfaceInfoService.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = userInterfaceInfoService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新调用关系（仅允许调整剩余次数与状态）
     *
     * @param userInterfaceInfoUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUserInterfaceInfo(@RequestBody UserInterfaceInfoUpdateRequest userInterfaceInfoUpdateRequest) {
        if (userInterfaceInfoUpdateRequest == null || userInterfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (userInterfaceInfoService.getById(userInterfaceInfoUpdateRequest.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
        BeanUtils.copyProperties(userInterfaceInfoUpdateRequest, userInterfaceInfo);
        // 只更新剩余次数与状态列，避免覆盖计数结果
        userInterfaceInfo.setUserId(null);
        userInterfaceInfo.setInterfaceInfoId(null);
        userInterfaceInfoService.validUserInterfaceInfo(userInterfaceInfo, false);
        boolean result = userInterfaceInfoService.updateById(userInterfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 充值调用次数：在剩余次数上按增量累加（次数用完后续费）
     *
     * @param userInterfaceInfoChargeRequest
     * @return 充值后的剩余调用次数
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/charge")
    public BaseResponse<Integer> chargeUserInterfaceInfo(@RequestBody UserInterfaceInfoChargeRequest userInterfaceInfoChargeRequest) {
        if (userInterfaceInfoChargeRequest == null
                || userInterfaceInfoChargeRequest.getId() == null
                || userInterfaceInfoChargeRequest.getNum() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        int leftNum = userInterfaceInfoService.chargeLeftNum(
                userInterfaceInfoChargeRequest.getId(), userInterfaceInfoChargeRequest.getNum());
        return ResultUtils.success(leftNum);
    }

    /**
     * 分页获取调用关系列表（含用户名称、接口名称、总调用次数、剩余调用次数）
     *
     * @param userInterfaceInfoQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list/page")
    public BaseResponse<IPage<UserInterfaceInfo>> listUserInterfaceInfoByPage(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest) {
        IPage<UserInterfaceInfo> page = userInterfaceInfoService.getUserInterfaceInfoPage(userInterfaceInfoQueryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 查询当前登录用户自己的调用次数（个人中心展示用）
     *
     * @param request
     * @return 调用关系列表（含接口名称）
     */
    @GetMapping("/my")
    public BaseResponse<List<UserInterfaceInfo>> listMyUserInterfaceInfo(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userInterfaceInfoService.listMyQuota(loginUser.getId()));
    }

    /**
     * 接口调用计数：总次数 +1、剩余次数 -1
     * <p>
     * 实际调用链路中的计数由接口服务（alanapi-interface）在验签通过后直接完成，
     * 此接口供内部链路接入或管理员手动核账使用
     *
     * @param userInterfaceInfoInvokeRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/invoke/count")
    public BaseResponse<Boolean> invokeCount(@RequestBody UserInterfaceInfoInvokeRequest userInterfaceInfoInvokeRequest) {
        if (userInterfaceInfoInvokeRequest == null
                || userInterfaceInfoInvokeRequest.getInterfaceInfoId() == null
                || userInterfaceInfoInvokeRequest.getUserId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userInterfaceInfoService.invokeCount(
                userInterfaceInfoInvokeRequest.getInterfaceInfoId(), userInterfaceInfoInvokeRequest.getUserId());
        return ResultUtils.success(result);
    }
}
