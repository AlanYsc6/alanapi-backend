package com.alan.project.service;

import com.alan.project.model.dto.invokelog.InvokeLogQueryRequest;
import com.alan.project.model.entity.InvokeLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 接口调用日志数据库操作Service
 *
 * @author alan
 */
public interface InvokeLogService extends IService<InvokeLog> {

    /**
     * 分页查询调用日志，并补充用户名称、接口名称
     *
     * @param invokeLogQueryRequest 查询条件
     * @return 分页结果
     */
    IPage<InvokeLog> getInvokeLogPage(InvokeLogQueryRequest invokeLogQueryRequest);

    /**
     * 记录被平台拒绝的调用（接口下线 / 缺少凭证 / 无法连接接口服务等），
     * 拒绝原因写入响应数据字段，不计次
     *
     * @param interfaceInfoId 接口 id（接口不存在时传请求的 id）
     * @param userId          调用用户 id
     * @param requestPath     请求路径
     * @param requestMethod   请求方式（未知传 null）
     * @param requestParams   请求参数
     * @param reason          拒绝原因
     */
    void recordRejected(Long interfaceInfoId, Long userId, String requestPath,
                        String requestMethod, String requestParams, String reason);
}
