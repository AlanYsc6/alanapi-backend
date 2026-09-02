package com.alan.project.manager;

import com.alan.project.common.ErrorCode;
import com.alan.project.config.TosConfig;
import com.alan.project.exception.BusinessException;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.TosException;
import com.volcengine.tos.model.object.ObjectMetaRequestOptions;
import com.volcengine.tos.model.object.PutObjectInput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * TOS 文件管理器，负责文件上传到火山引擎对象存储
 *
 * @author alan
 */
@Slf4j
@Component
public class TosFileManager {

    @Resource
    private TosConfig tosConfig;

    private TOSV2 tosClient;

    @PostConstruct
    public void init() {
        // 参数顺序以 SDK 字节码为准：build(region, endpoint, accessKey, secretKey)
        tosClient = new TOSV2ClientBuilder().build(
                tosConfig.getRegion(),
                tosConfig.getEndpoint(),
                tosConfig.getAccessKey(),
                tosConfig.getSecretKey());
    }

    /**
     * 上传图片，返回可公开访问的 URL
     *
     * @param file 用户上传的图片文件
     * @return 图片 URL
     */
    public String uploadImage(MultipartFile file) {
        // 1. 校验文件
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
        }
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType) || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持上传图片文件");
        }
        // 2. 生成唯一存储路径：avatar/yyyyMM/uuid.ext
        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String key = String.format("avatar/%s/%s.%s",
                new SimpleDateFormat("yyyyMM").format(new Date()),
                UUID.randomUUID().toString().replace("-", ""),
                extension);
        return upload(file, key, contentType);
    }

    /**
     * 上传通用文件（SDK 包等，不限制文件类型），返回可公开访问的 URL
     *
     * @param file 待上传文件
     * @param dir  存储目录（如 sdk）
     * @return 文件 URL
     */
    public String uploadFile(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
        }
        String contentType = StringUtils.defaultIfBlank(file.getContentType(), "application/octet-stream");
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.lastIndexOf('.') >= 0
                ? StringUtils.lowerCase(originalFilename.substring(originalFilename.lastIndexOf('.') + 1))
                : "";
        String key = String.format("%s/%s/%s%s",
                dir,
                new SimpleDateFormat("yyyyMM").format(new Date()),
                UUID.randomUUID().toString().replace("-", ""),
                StringUtils.isNotBlank(extension) ? "." + extension : "");
        return upload(file, key, contentType);
    }

    /**
     * 上传到 TOS 并拼接访问地址
     */
    private String upload(MultipartFile file, String key, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectInput input = new PutObjectInput();
            input.setBucket(tosConfig.getBucketName());
            input.setKey(key);
            input.setContent(inputStream);
            input.setContentLength(file.getSize());
            input.setOptions(new ObjectMetaRequestOptions().setContentType(contentType));
            tosClient.putObject(input);
        } catch (IOException | TosException e) {
            log.error("文件上传到 TOS 失败, key = {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }
        String baseUrl = StringUtils.isNotBlank(tosConfig.getDomain())
                ? tosConfig.getDomain()
                : String.format("https://%s.%s", tosConfig.getBucketName(), tosConfig.getEndpoint());
        return baseUrl + "/" + key;
    }

    /**
     * 优先取原文件名后缀，取不到时根据 Content-Type 推断
     */
    private String resolveExtension(String originalFilename, String contentType) {
        if (StringUtils.isNotBlank(originalFilename) && originalFilename.lastIndexOf('.') >= 0) {
            return StringUtils.lowerCase(originalFilename.substring(originalFilename.lastIndexOf('.') + 1));
        }
        return StringUtils.lowerCase(contentType.replace("image/", ""));
    }
}
