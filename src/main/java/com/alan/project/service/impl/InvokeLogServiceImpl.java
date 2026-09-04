package com.alan.project.service.impl;

import com.alan.project.common.ErrorCode;
import com.alan.project.exception.BusinessException;
import com.alan.project.mapper.InvokeLogMapper;
import com.alan.project.model.dto.invokelog.InvokeLogQueryRequest;
import com.alan.project.model.entity.InterfaceInfo;
import com.alan.project.model.entity.InvokeLog;
import com.alan.project.model.entity.User;
import com.alan.project.service.InterfaceInfoService;
import com.alan.project.service.InvokeLogService;
import com.alan.project.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 接口调用日志数据库操作Service实现
 *
 * @author alan
 */
@Service
public class InvokeLogServiceImpl extends ServiceImpl<InvokeLogMapper, InvokeLog>
        implements InvokeLogService {

    @Resource
    private UserService userService;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Override
    public IPage<InvokeLog> getInvokeLogPage(InvokeLogQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InvokeLog invokeLogQuery = new InvokeLog();
        invokeLogQuery.setUserId(queryRequest.getUserId());
        invokeLogQuery.setInterfaceInfoId(queryRequest.getInterfaceInfoId());
        invokeLogQuery.setStatus(queryRequest.getStatus());
        QueryWrapper<InvokeLog> queryWrapper = new QueryWrapper<>(invokeLogQuery);
        queryWrapper.orderByDesc("createTime");
        Page<InvokeLog> page = page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()), queryWrapper);
        fillRecordInfo(page.getRecords());
        return page;
    }

    /**
     * 批量补充用户名称、接口名称，供列表展示
     */
    private void fillRecordInfo(List<InvokeLog> recordList) {
        if (recordList == null || recordList.isEmpty()) {
            return;
        }
        Set<Long> userIds = recordList.stream()
                .map(InvokeLog::getUserId).collect(Collectors.toSet());
        Set<Long> interfaceInfoIds = recordList.stream()
                .map(InvokeLog::getInterfaceInfoId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, InterfaceInfo> interfaceInfoMap = interfaceInfoIds.isEmpty() ? Collections.emptyMap()
                : interfaceInfoService.listByIds(interfaceInfoIds).stream()
                        .collect(Collectors.toMap(InterfaceInfo::getId, Function.identity()));
        recordList.forEach(record -> {
            User user = userMap.get(record.getUserId());
            if (user != null) {
                record.setUserName(user.getUserName());
            }
            InterfaceInfo interfaceInfo = interfaceInfoMap.get(record.getInterfaceInfoId());
            if (interfaceInfo != null) {
                record.setInterfaceName(interfaceInfo.getName());
            }
        });
    }
}
