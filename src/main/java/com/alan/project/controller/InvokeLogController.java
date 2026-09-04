package com.alan.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.alan.project.annotation.AuthCheck;
import com.alan.project.common.BaseResponse;
import com.alan.project.common.DeleteRequest;
import com.alan.project.common.ErrorCode;
import com.alan.project.common.ResultUtils;
import com.alan.project.exception.BusinessException;
import com.alan.project.model.dto.invokelog.InvokeLogQueryRequest;
import com.alan.project.model.entity.InvokeLog;
import com.alan.project.service.InvokeLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 接口调用日志接口（日志由接口服务写入，此处仅提供管理员查询）
 *
 * @author alan
 */
@RestController
@RequestMapping("/invokeLog")
@Slf4j
public class InvokeLogController {

    @Resource
    private InvokeLogService invokeLogService;

    /**
     * 分页获取调用日志列表（含用户名称、接口名称、调用状态、耗时）
     *
     * @param invokeLogQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list/page")
    public BaseResponse<IPage<InvokeLog>> listInvokeLogByPage(InvokeLogQueryRequest invokeLogQueryRequest) {
        IPage<InvokeLog> page = invokeLogService.getInvokeLogPage(invokeLogQueryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 删除调用日志
     *
     * @param deleteRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInvokeLog(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        if (invokeLogService.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = invokeLogService.removeById(id);
        return ResultUtils.success(result);
    }
}
