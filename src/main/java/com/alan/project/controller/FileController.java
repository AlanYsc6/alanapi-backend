package com.alan.project.controller;

import com.alan.project.common.BaseResponse;
import com.alan.project.common.ResultUtils;
import com.alan.project.manager.TosFileManager;
import com.alan.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 文件接口
 *
 * @author alan
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private TosFileManager tosFileManager;

    @Resource
    private UserService userService;

    /**
     * 上传图片（需登录）
     *
     * @param file 图片文件
     * @return 图片访问 URL
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestParam("file") MultipartFile file,
                                           HttpServletRequest request) {
        // 仅登录用户可上传
        userService.getLoginUser(request);
        return ResultUtils.success(tosFileManager.uploadImage(file));
    }
}
