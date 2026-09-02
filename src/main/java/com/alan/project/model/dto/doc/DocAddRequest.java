package com.alan.project.model.dto.doc;

import lombok.Data;

import java.io.Serializable;

/**
 * 文档创建请求
 *
 * @author alan
 */
@Data
public class DocAddRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容（支持 ## 小标题、``` 代码块）
     */
    private String content;

    /**
     * 展示顺序（越小越靠前）
     */
    private Integer sort;

    private static final long serialVersionUID = 1L;
}
