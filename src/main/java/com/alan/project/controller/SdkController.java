package com.alan.project.controller;

import com.alan.project.annotation.AuthCheck;
import com.alan.project.common.BaseResponse;
import com.alan.project.common.DeleteRequest;
import com.alan.project.common.ErrorCode;
import com.alan.project.common.ResultUtils;
import com.alan.project.exception.BusinessException;
import com.alan.project.manager.TosFileManager;
import com.alan.project.model.dto.sdk.SdkAddRequest;
import com.alan.project.model.dto.sdk.SdkUpdateRequest;
import com.alan.project.model.entity.Sdk;
import com.alan.project.service.SdkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

/**
 * SDK 下载接口
 *
 * @author alan
 */
@RestController
@RequestMapping("/sdk")
@Slf4j
public class SdkController {

    @Resource
    private SdkService sdkService;

    @Resource
    private TosFileManager tosFileManager;

    /**
     * 上传 SDK 文件到对象存储（仅管理员可使用），返回文件 URL
     *
     * @param file SDK 文件（jar/zip 等）
     * @return 文件 URL
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/upload")
    public BaseResponse<String> uploadSdk(@RequestParam("file") MultipartFile file) {
        return ResultUtils.success(tosFileManager.uploadFile(file, "sdk"));
    }

    // region 增删改查

    /**
     * 创建（仅管理员可使用）
     *
     * @param sdkAddRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/add")
    public BaseResponse<Long> addSdk(@RequestBody SdkAddRequest sdkAddRequest) {
        if (sdkAddRequest == null || StringUtils.isAnyBlank(sdkAddRequest.getName(), sdkAddRequest.getFileUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称和文件不能为空");
        }
        Sdk sdk = new Sdk();
        BeanUtils.copyProperties(sdkAddRequest, sdk);
        boolean result = sdkService.save(sdk);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(sdk.getId());
    }

    /**
     * 删除（仅管理员可使用）
     *
     * @param deleteRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSdk(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        if (sdkService.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = sdkService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新（仅管理员可使用）
     *
     * @param sdkUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSdk(@RequestBody SdkUpdateRequest sdkUpdateRequest) {
        if (sdkUpdateRequest == null || sdkUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isAnyBlank(sdkUpdateRequest.getName(), sdkUpdateRequest.getFileUrl())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称和文件不能为空");
        }
        // 判断是否存在
        if (sdkService.getById(sdkUpdateRequest.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Sdk sdk = new Sdk();
        BeanUtils.copyProperties(sdkUpdateRequest, sdk);
        boolean result = sdkService.updateById(sdk);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Sdk> getSdkById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(sdkService.getById(id));
    }

    /**
     * 获取全部 SDK（按创建时间倒序），首页下载列表用
     *
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<Sdk>> listSdk() {
        QueryWrapper<Sdk> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("createTime", "id");
        return ResultUtils.success(sdkService.list(queryWrapper));
    }

    // endregion

}
