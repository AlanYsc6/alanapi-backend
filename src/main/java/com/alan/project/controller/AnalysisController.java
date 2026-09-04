package com.alan.project.controller;

import com.alan.project.annotation.AuthCheck;
import com.alan.project.common.BaseResponse;
import com.alan.project.common.ResultUtils;
import com.alan.project.mapper.InterfaceInfoMapper;
import com.alan.project.mapper.InvokeLogMapper;
import com.alan.project.model.vo.InterfaceInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 接口分析（仅管理员）：调用排行、调用趋势、调用总览
 * <p>
 * 排行来自 user_interface_info 的累计计数；趋势与总览来自 invoke_log 调用日志
 *
 * @author alan
 */
@RestController
@RequestMapping("/analysis")
@Slf4j
public class AnalysisController {

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;

    @Resource
    private InvokeLogMapper invokeLogMapper;

    /**
     * 调用次数最多的接口排行（TOP 10）
     *
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/top/interface/invoke")
    public BaseResponse<List<InterfaceInfoVO>> listTopInvokeInterfaceInfo() {
        return ResultUtils.success(interfaceInfoMapper.listTopInvokeInterfaceInfo(10));
    }

    /**
     * 近 N 天调用趋势（按天统计，默认 30 天）
     *
     * @param days
     * @return [{date, count}]，无调用的日期补 0
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/invoke/trend")
    public BaseResponse<List<Map<String, Object>>> listInvokeTrend(
            @RequestParam(defaultValue = "30") int days) {
        if (days <= 0 || days > 365) {
            days = 30;
        }
        String since = LocalDate.now().minusDays(days - 1L).toString();
        List<Map<String, Object>> trend = invokeLogMapper.listInvokeTrend(since);
        return ResultUtils.success(trend);
    }

    /**
     * 调用总览：总调用次数、成功次数、平均耗时、调用用户数
     *
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/invoke/overview")
    public BaseResponse<Map<String, Object>> getInvokeOverview() {
        return ResultUtils.success(invokeLogMapper.getInvokeOverview());
    }
}
