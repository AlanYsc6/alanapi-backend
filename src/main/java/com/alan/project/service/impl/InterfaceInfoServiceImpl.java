package com.alan.project.service.impl;

import com.alan.project.common.ErrorCode;
import com.alan.project.exception.BusinessException;
import com.alan.project.mapper.InterfaceInfoMapper;
import com.alan.project.model.entity.InterfaceInfo;
import com.alan.project.model.enums.InterfaceInfoStatusEnum;
import com.alan.project.service.InterfaceInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
* @author 19011
* @description 针对表【interface_info(接口信息)】的数据库操作Service实现
* @createDate 2026-08-30 00:39:44
*/
@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
    implements InterfaceInfoService{

    @Override
    public void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add) {
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceInfo.getName();
        String description = interfaceInfo.getDescription();
        String url = interfaceInfo.getUrl();
        String requestParams = interfaceInfo.getRequestParams();
        String requestBody = interfaceInfo.getRequestBody();
        String method = interfaceInfo.getMethod();
        Integer status = interfaceInfo.getStatus();
        // 创建时，核心参数必须非空，请求参数与请求体至少填一项
        if (add) {
            if (StringUtils.isAnyBlank(name, url, method)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
            if (StringUtils.isBlank(requestParams) && StringUtils.isBlank(requestBody)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数与请求体至少填写一项");
            }
        }
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
        if (StringUtils.isNotBlank(description) && description.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "描述过长");
        }
        if (StringUtils.isNotBlank(url)) {
            if (url.length() > 512) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址过长");
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址格式错误");
            }
        }
        if (StringUtils.isNotBlank(method)
                && !Arrays.asList("GET", "POST").contains(method.toUpperCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求类型仅支持 GET/POST");
        }
        if (status != null && !InterfaceInfoStatusEnum.getValues().contains(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口状态不合法");
        }
    }

}
