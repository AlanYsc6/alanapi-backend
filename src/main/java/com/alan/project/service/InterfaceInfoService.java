package com.alan.project.service;

import com.alan.project.model.entity.InterfaceInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 19011
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2026-08-30 00:39:44
*/
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    /**
     * 校验接口信息是否合法
     *
     * @param interfaceInfo
     * @param add 是否为创建校验（创建时核心参数必须非空）
     */
    void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add);
}
