package com.alan.project.service;

import com.alan.project.model.dto.userinterfaceinfo.UserInterfaceInfoQueryRequest;
import com.alan.project.model.entity.UserInterfaceInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author 19011
* @description 针对表【user_interface_info(用户调用接口关系)】的数据库操作Service
* @createDate 2026-09-04 11:44:24
*/
public interface UserInterfaceInfoService extends IService<UserInterfaceInfo> {

    /**
     * 校验调用关系参数是否合法
     *
     * @param userInterfaceInfo 调用关系
     * @param add               是否为创建校验
     */
    void validUserInterfaceInfo(UserInterfaceInfo userInterfaceInfo, boolean add);

    /**
     * 接口调用计数：总次数 +1、剩余次数 -1（条件更新，剩余次数不足 / 已禁用时计数失败）
     *
     * @param interfaceInfoId 接口 id
     * @param userId          调用用户 id
     * @return true 计数成功；false 无可用次数或记录被禁用
     */
    boolean invokeCount(long interfaceInfoId, long userId);

    /**
     * 充值剩余调用次数：在当前剩余次数上按增量累加
     *
     * @param id  调用关系主键
     * @param num 充值次数，必须大于 0
     * @return 充值后的剩余调用次数
     */
    int chargeLeftNum(long id, int num);

    /**
     * 分页查询调用关系，并补充用户名称、接口名称
     *
     * @param userInterfaceInfoQueryRequest 查询条件
     * @return 分页结果
     */
    IPage<UserInterfaceInfo> getUserInterfaceInfoPage(UserInterfaceInfoQueryRequest userInterfaceInfoQueryRequest);

    /**
     * 查询用户开通的接口调用次数（含接口名称，个人中心展示用）
     *
     * @param userId 调用用户 id
     * @return 调用关系列表
     */
    List<UserInterfaceInfo> listMyQuota(long userId);
}
