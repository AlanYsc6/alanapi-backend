package com.alan.project.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alan.alanapiclientsdk.client.AlanApiClient;
import com.alan.alanapiclientsdk.exception.ApiException;
import com.alan.alanapiclientsdk.utils.SignUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alan.project.annotation.AuthCheck;
import com.alan.project.common.BaseResponse;
import com.alan.project.common.DeleteRequest;
import com.alan.project.common.ErrorCode;
import com.alan.project.common.ResultUtils;
import com.alan.project.constant.CommonConstant;
import com.alan.project.exception.BusinessException;
import com.alan.project.model.dto.IdRequest;
import com.alan.project.model.dto.interfaceinfo.InterfaceInfoAddRequest;
import com.alan.project.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.alan.project.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.alan.project.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.alan.project.model.entity.InterfaceInfo;
import com.alan.project.model.entity.User;
import com.alan.project.model.enums.InterfaceInfoStatusEnum;
import com.alan.project.service.InterfaceInfoService;
import com.alan.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 接口信息接口
 *
 * @author alan
 */
@RestController
@RequestMapping("/interfaceInfo")
@Slf4j
public class InterfaceInfoController {

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Resource
    private UserService userService;

    /**
     * 接口服务统一响应中的成功 code（与 SDK AlanApiClient 的 SUCCESS_CODE 一致）
     */
    private static final int INTERFACE_SUCCESS_CODE = 200;

    // region 增删改查

    /**
     * 创建
     *
     * @param interfaceInfoAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addInterfaceInfo(@RequestBody InterfaceInfoAddRequest interfaceInfoAddRequest, HttpServletRequest request) {
        if (interfaceInfoAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoAddRequest, interfaceInfo);
        // 校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, true);
        User loginUser = userService.getLoginUser(request);
        interfaceInfo.setUserId(loginUser.getId());
        boolean result = interfaceInfoService.save(interfaceInfo);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        long newInterfaceInfoId = interfaceInfo.getId();
        return ResultUtils.success(newInterfaceInfoId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteInterfaceInfo(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = interfaceInfoService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新
     *
     * @param interfaceInfoUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateInterfaceInfo(@RequestBody InterfaceInfoUpdateRequest interfaceInfoUpdateRequest,
                                            HttpServletRequest request) {
        if (interfaceInfoUpdateRequest == null || interfaceInfoUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        // 状态只能通过 /online、/offline 修改，update 一律忽略，避免创建者绕过管理员自行发布
        interfaceInfo.setStatus(null);
        // 参数校验
        interfaceInfoService.validInterfaceInfo(interfaceInfo, false);
        User user = userService.getLoginUser(request);
        long id = interfaceInfoUpdateRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可修改
        if (!oldInterfaceInfo.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<InterfaceInfo> getInterfaceInfoById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(id);
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 非管理员仅可查看已上线的接口
        if (!userService.isAdmin(request)
                && !Objects.equals(interfaceInfo.getStatus(), InterfaceInfoStatusEnum.ONLINE.getValue())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(interfaceInfo);
    }

    /**
     * 获取列表（仅管理员可使用）
     *
     * @param interfaceInfoQueryRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list")
    public BaseResponse<List<InterfaceInfo>> listInterfaceInfo(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        if (interfaceInfoQueryRequest != null) {
            BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        List<InterfaceInfo> interfaceInfoList = interfaceInfoService.list(queryWrapper);
        return ResultUtils.success(interfaceInfoList);
    }

    /**
     * 分页获取列表
     *
     * @param interfaceInfoQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<InterfaceInfo>> listInterfaceInfoByPage(InterfaceInfoQueryRequest interfaceInfoQueryRequest, HttpServletRequest request) {
        if (interfaceInfoQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfoQuery = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoQueryRequest, interfaceInfoQuery);
        long current = interfaceInfoQueryRequest.getCurrent();
        long size = interfaceInfoQueryRequest.getPageSize();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        String description = interfaceInfoQuery.getDescription();
        // description 需支持模糊搜索
        interfaceInfoQuery.setDescription(null);
        // 限制爬虫
        if (size > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>(interfaceInfoQuery);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField),
            CommonConstant.SORT_ORDER_ASC.equals(sortOrder), sortField);
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoService.page(new Page<>(current, size), queryWrapper);
        return ResultUtils.success(interfaceInfoPage);
    }

    /**
     * 发布（仅管理员可使用）
     *
     * @param idRequest
     * @param request
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/online")
    public BaseResponse<Boolean> onlineInterfaceInfo(@RequestBody IdRequest idRequest, HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 判断该接口是否可以调用（发布前先试调用一次接口服务）
        invokeTest(oldInterfaceInfo, request);
        // 只更新状态列，避免回写其他字段
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.ONLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 发布前的试调用验证：用当前操作管理员的 ak/sk 签名，按接口信息中的地址、请求类型和参数
     * 构造真实请求调用接口服务，调用通过（统一响应 code = 200 且 data 非空）才允许发布上线
     * <p>
     * 试调用参数约定：
     * 1. 请求体（requestBody）为 JSON 对象且非 GET 请求时，作为 JSON 请求体调用，如 {"username": "test"}
     * 2. 否则从请求参数（requestParams）解析 JSON 对象作为表单参数，如 {"name": "test"}；
     *    表单接口参与签名的是第一个参数的值（与接口服务端 @SignCheck(bodyParam) 的约定一致）
     */
    private void invokeTest(InterfaceInfo interfaceInfo, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();
        if (StringUtils.isAnyBlank(accessKey, secretKey)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "当前账号缺少开放平台调用凭证，无法完成发布验证");
        }
        String url = interfaceInfo.getUrl();
        if (StringUtils.isBlank(url)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口地址为空，无法完成发布验证");
        }
        boolean isGet = "GET".equalsIgnoreCase(interfaceInfo.getMethod());
        // 解析试调用参数：优先 JSON 请求体，其次 JSON 表单参数
        String jsonBody = null;
        JSONObject formParams = null;
        String requestBody = interfaceInfo.getRequestBody();
        if (!isGet && StringUtils.isNotBlank(requestBody) && JSONUtil.isTypeJSONObject(requestBody)) {
            jsonBody = requestBody;
        }
        if (jsonBody == null) {
            String requestParams = interfaceInfo.getRequestParams();
            if (StringUtils.isNotBlank(requestParams) && JSONUtil.isTypeJSONObject(requestParams)) {
                formParams = JSONUtil.parseObj(requestParams);
            }
            if (formParams == null || formParams.isEmpty()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "无法构造试调用参数，请在接口的请求参数或请求体中填写 JSON 格式的示例参数");
            }
        }
        // 参与签名的 body：JSON 请求签原始 JSON 串，表单请求签第一个参数值
        String signBody;
        if (jsonBody != null) {
            signBody = jsonBody;
        } else {
            Object firstParamValue = formParams.values().iterator().next();
            signBody = firstParamValue == null ? "" : String.valueOf(firstParamValue);
        }
        // 构造签名请求头，secretKey 只参与本地签名计算，不随请求发送
        Map<String, String> signParams = new HashMap<>();
        signParams.put("accessKey", accessKey);
        signParams.put("body", signBody);
        signParams.put("nonce", IdUtil.simpleUUID());
        signParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        String sign = SignUtils.genSign(signParams, secretKey);
        // 按接口信息构造请求
        HttpRequest httpRequest = isGet ? HttpRequest.get(url) : HttpRequest.post(url);
        if (jsonBody != null) {
            httpRequest.body(jsonBody);
        } else {
            httpRequest.form(formParams);
        }
        httpRequest.header("accessKey", accessKey);
        httpRequest.header("nonce", signParams.get("nonce"));
        httpRequest.header("timestamp", signParams.get("timestamp"));
        httpRequest.header("sign", sign);
        httpRequest.setConnectionTimeout(5000);
        httpRequest.setReadTimeout(10000);
        // 发送并解析服务端的统一响应结构
        String responseBody;
        try (HttpResponse response = httpRequest.execute()) {
            responseBody = response.body();
        } catch (Exception e) {
            log.error("发布验证调用失败, interfaceInfoId = {}, url = {}", interfaceInfo.getId(), url, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口验证失败：无法连接接口服务");
        }
        if (responseBody == null || !JSONUtil.isTypeJSONObject(responseBody)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "接口验证失败：接口响应格式异常，请检查接口地址 " + url + " 是否正确");
        }
        JSONObject result = JSONUtil.parseObj(responseBody);
        int code = result.getInt("code", -1);
        if (code != INTERFACE_SUCCESS_CODE) {
            log.error("发布验证调用失败, interfaceInfoId = {}, url = {}, code = {}, message = {}",
                    interfaceInfo.getId(), url, code, result.getStr("message"));
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口验证失败：" + result.getStr("message", ""));
        }
        if (StringUtils.isBlank(result.getStr("data"))) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口验证失败：接口返回数据为空");
        }
        log.info("发布验证调用通过, interfaceInfoId = {}, url = {}, data = {}",
                interfaceInfo.getId(), url, result.getStr("data"));
    }

    /**
     * 下线（仅管理员可使用）
     *
     * @param idRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/offline")
    public BaseResponse<Boolean> offlineInterfaceInfo(@RequestBody IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = idRequest.getId();
        // 判断是否存在
        InterfaceInfo oldInterfaceInfo = interfaceInfoService.getById(id);
        if (oldInterfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 只更新状态列，避免回写其他字段
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.OFFLINE.getValue());
        boolean result = interfaceInfoService.updateById(interfaceInfo);
        return ResultUtils.success(result);
    }

    /**
     * 调用接口（使用当前登录用户的 ak/sk 发起签名调用）
     *
     * @param interfaceInfoInvokeRequest
     * @param request
     * @return
     */
    @PostMapping("/invoke")
    public BaseResponse<Object> invokeInterfaceInfo(@RequestBody InterfaceInfoInvokeRequest interfaceInfoInvokeRequest,
                                                    HttpServletRequest request) {
        if (interfaceInfoInvokeRequest == null || interfaceInfoInvokeRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        InterfaceInfo interfaceInfo = interfaceInfoService.getById(interfaceInfoInvokeRequest.getId());
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!Objects.equals(interfaceInfo.getStatus(), InterfaceInfoStatusEnum.ONLINE.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口已下线");
        }
        User loginUser = userService.getLoginUser(request);
        String accessKey = loginUser.getAccessKey();
        String secretKey = loginUser.getSecretKey();
        if (StringUtils.isAnyBlank(accessKey, secretKey)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "缺少开放平台调用凭证");
        }
        // 当前开放接口服务仅提供 name API，从用户参数中取出 name 参与调用与签名
        String name = interfaceInfoInvokeRequest.getUserRequestParams();
        if (StringUtils.isNotBlank(name) && JSONUtil.isTypeJSONObject(name)) {
            name = JSONUtil.parseObj(name).getStr("name", name);
        }
        AlanApiClient alanApiClient = new AlanApiClient(accessKey, secretKey);
        String result;
        try {
            result = "GET".equalsIgnoreCase(interfaceInfo.getMethod())
                    ? alanApiClient.getNameByGet(name)
                    : alanApiClient.getNameByPost(name);
        } catch (ApiException e) {
            log.error("接口调用失败, interfaceInfoId = {}, code: {}, message: {}",
                    interfaceInfo.getId(), e.getCode(), e.getMessage());
            throw new BusinessException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("接口调用失败, interfaceInfoId = {}", interfaceInfo.getId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口调用失败");
        }
        return ResultUtils.success(result);
    }

    // endregion

}
