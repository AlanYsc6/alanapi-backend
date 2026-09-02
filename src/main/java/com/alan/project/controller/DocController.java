package com.alan.project.controller;

import com.alan.project.annotation.AuthCheck;
import com.alan.project.common.BaseResponse;
import com.alan.project.common.DeleteRequest;
import com.alan.project.common.ErrorCode;
import com.alan.project.common.ResultUtils;
import com.alan.project.exception.BusinessException;
import com.alan.project.model.dto.doc.DocAddRequest;
import com.alan.project.model.dto.doc.DocUpdateRequest;
import com.alan.project.model.entity.Doc;
import com.alan.project.service.DocService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 首页文档接口
 *
 * @author alan
 */
@RestController
@RequestMapping("/doc")
@Slf4j
public class DocController {

    @Resource
    private DocService docService;

    // region 增删改查

    /**
     * 创建（仅管理员可使用）
     *
     * @param docAddRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/add")
    public BaseResponse<Long> addDoc(@RequestBody DocAddRequest docAddRequest) {
        if (docAddRequest == null || StringUtils.isBlank(docAddRequest.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        Doc doc = new Doc();
        BeanUtils.copyProperties(docAddRequest, doc);
        if (doc.getSort() == null) {
            doc.setSort(0);
        }
        boolean result = docService.save(doc);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(doc.getId());
    }

    /**
     * 删除（仅管理员可使用）
     *
     * @param deleteRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteDoc(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        if (docService.getById(id) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        boolean result = docService.removeById(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新（仅管理员可使用）
     *
     * @param docUpdateRequest
     * @return
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateDoc(@RequestBody DocUpdateRequest docUpdateRequest) {
        if (docUpdateRequest == null || docUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(docUpdateRequest.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题不能为空");
        }
        // 判断是否存在
        if (docService.getById(docUpdateRequest.getId()) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        Doc doc = new Doc();
        BeanUtils.copyProperties(docUpdateRequest, doc);
        if (doc.getSort() == null) {
            doc.setSort(0);
        }
        boolean result = docService.updateById(doc);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Doc> getDocById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return ResultUtils.success(docService.getById(id));
    }

    /**
     * 获取全部文档（按 sort 升序），首页展示用
     *
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<Doc>> listDoc() {
        QueryWrapper<Doc> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("sort", "id");
        return ResultUtils.success(docService.list(queryWrapper));
    }

    // endregion

}
