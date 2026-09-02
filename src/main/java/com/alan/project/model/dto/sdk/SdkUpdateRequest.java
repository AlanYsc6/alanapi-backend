package com.alan.project.model.dto.sdk;

import lombok.Data;

import java.io.Serializable;

/**
 * SDK 更新请求
 *
 * @author alan
 */
@Data
public class SdkUpdateRequest implements Serializable {

    /**
     * 主键
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 版本号
     */
    private String version;

    /**
     * 说明
     */
    private String description;

    /**
     * 文件地址（重新上传时更新）
     */
    private String fileUrl;

    private static final long serialVersionUID = 1L;
}
