package com.alan.project.model.dto.sdk;

import lombok.Data;

import java.io.Serializable;

/**
 * SDK 创建请求
 *
 * @author alan
 */
@Data
public class SdkAddRequest implements Serializable {

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
     * 文件地址（先调用 /sdk/upload 上传获得）
     */
    private String fileUrl;

    private static final long serialVersionUID = 1L;
}
