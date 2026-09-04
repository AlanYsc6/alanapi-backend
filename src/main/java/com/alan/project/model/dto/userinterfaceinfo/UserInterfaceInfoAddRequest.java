package com.alan.project.model.dto.userinterfaceinfo;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建用户调用接口关系请求（管理员为用户分配接口调用次数）
 *
 * @author alan
 */
@Data
public class UserInterfaceInfoAddRequest implements Serializable {

    /**
     * 调用用户 id
     */
    private Long userId;

    /**
     * 接口 id
     */
    private Long interfaceInfoId;

    /**
     * 分配的初始调用次数
     */
    private Integer leftNum;

    private static final long serialVersionUID = 1L;
}
