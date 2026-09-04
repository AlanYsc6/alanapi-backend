package com.alan.project.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 接口调用统计视图（接口分析用）
 *
 * @author alan
 */
@Data
public class InterfaceInfoVO implements Serializable {

    /**
     * 接口 id
     */
    private Long id;

    /**
     * 接口名称
     */
    private String name;

    /**
     * 总调用次数
     */
    private Long totalNum;

    private static final long serialVersionUID = 1L;
}
