package com.alan.project.model.dto.userinterfaceinfo;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口调用计数请求（供内部链路或管理员手动修正调用次数使用）
 *
 * @author alan
 */
@Data
public class UserInterfaceInfoInvokeRequest implements Serializable {

    /**
     * 调用用户 id
     */
    private Long userId;

    /**
     * 接口 id
     */
    private Long interfaceInfoId;

    private static final long serialVersionUID = 1L;
}
