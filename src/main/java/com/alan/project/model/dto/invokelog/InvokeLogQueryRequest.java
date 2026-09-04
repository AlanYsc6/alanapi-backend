package com.alan.project.model.dto.invokelog;

import com.alan.project.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询接口调用日志请求
 *
 * @author alan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InvokeLogQueryRequest extends PageRequest implements Serializable {

    /**
     * 调用用户 id
     */
    private Long userId;

    /**
     * 接口 id
     */
    private Long interfaceInfoId;

    /**
     * 调用状态（0-失败，1-成功）
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
