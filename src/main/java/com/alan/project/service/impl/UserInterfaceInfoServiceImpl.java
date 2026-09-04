package com.alan.project.service.impl;

import com.alan.project.common.ErrorCode;
import com.alan.project.exception.BusinessException;
import com.alan.project.mapper.UserInterfaceInfoMapper;
import com.alan.project.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.alan.project.model.entity.InterfaceInfo;
import com.alan.project.model.entity.User;
import com.alan.project.model.entity.UserInterfaceInfo;
import com.alan.project.service.InterfaceInfoService;
import com.alan.project.service.UserService;
import com.alan.project.service.UserInterfaceInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author 19011
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service实现
* @createDate 2026-09-04 11:44:24
*/
@Service
public class UserInterfaceInfoServiceImpl extends ServiceImpl<UserInterfaceInfoMapper, UserInterfaceInfo>
    implements UserInterfaceInfoService {

    @Resource
    private UserService userService;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Override
    public void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add) {
        if (userInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = userInterfaceInfo.getUserId();
        Long interfaceInfoId = userInterfaceInfo.getInterfaceInfoId();
        Integer leftNum = userInterfaceInfo.getLeftNum();
        Integer status = userInterfaceInfo.getStatus();
        if (add) {
            if (userId == null || userId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "调用用户不合法");
            }
            if (interfaceInfoId == null || interfaceInfoId <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口不合法");
            }
            if (leftNum == null || leftNum < 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始调用次数必须大于等于 0");
            }
        }
        if (leftNum != null && leftNum < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "剩余调用次数必须大于等于 0");
        }
        if (status != null && !Objects.equals(status, 0) && !Objects.equals(status, 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不合法");
        }
    }

    @Override
    public boolean invokeCount(long interfaceInfoId, long userId) {
        if (interfaceInfoId <= 0 || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 条件更新原子扣次：并发下剩余次数不会被扣成负数
        return baseMapper.countOnce(userId, interfaceInfoId) > 0;
    }

    @Override
    public int chargeLeftNum(long id, int num) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "调用关系 id 不合法");
        }
        if (num <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "充值次数必须大于 0");
        }
        if (this.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 按增量原子累加，不依赖读取当前剩余值，并发充值不会互相覆盖
        if (baseMapper.increaseLeftNum(id, num) <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "充值失败，请重试");
        }
        UserInterfaceInfo userInterfaceInfo = this.getById(id);
        return userInterfaceInfo.getLeftNum();
    }

    @Override
    public IPage<UserInterfaceInfo> getUserInterfaceInfoPage(UserInterfaceInfoQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserInterfaceInfo userInterfaceInfoQuery = new UserInterfaceInfo();
        userInterfaceInfoQuery.setUserId(queryRequest.getUserId());
        userInterfaceInfoQuery.setInterfaceInfoId(queryRequest.getInterfaceInfoId());
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>(userInterfaceInfoQuery);
        queryWrapper.orderByDesc("updateTime");
        Page<UserInterfaceInfo> page = page(new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()), queryWrapper);
        fillRecordInfo(page.getRecords());
        return page;
    }

    @Override
    public List<UserInterfaceInfo> listMyQuota(long userId) {
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LambdaQueryWrapper<UserInterfaceInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInterfaceInfo::getUserId, userId);
        queryWrapper.orderByDesc(UserInterfaceInfo::getUpdateTime);
        List<UserInterfaceInfo> recordList = this.list(queryWrapper);
        fillRecordInfo(recordList);
        return recordList;
    }

    /**
     * 批量补充用户名称、接口名称，供列表展示
     */
    private void fillRecordInfo(List<UserInterfaceInfo> recordList) {
        if (recordList == null || recordList.isEmpty()) {
            return;
        }
        Set<Long> userIds = recordList.stream()
                .map(UserInterfaceInfo::getUserId).collect(Collectors.toSet());
        Set<Long> interfaceInfoIds = recordList.stream()
                .map(UserInterfaceInfo::getInterfaceInfoId).collect(Collectors.toSet());
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
