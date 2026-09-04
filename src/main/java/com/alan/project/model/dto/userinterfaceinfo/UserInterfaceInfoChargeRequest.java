package com.alan.project.model.dto.userinterfaceinfo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户接口调用次数充值请求（在剩余次数基础上按增量累加）
 *
 * @author alan
 */
@Data
public class UserInterfaceInfoChargeRequest implements Serializable {

    /**
     * 调用关系主键
     */
    private Long id;

    /**
     * 充值的调用次数（必须大于 0）
     */
    private Integer num;

    private static final long serialVersionUID = 1L;
}
