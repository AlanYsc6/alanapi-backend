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
}
