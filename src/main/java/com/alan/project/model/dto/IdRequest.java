package com.alan.project.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用 Id 请求
 *
 * @author alan
 */
@Data
public class IdRequest implements Serializable {

    /**
     * 主键
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
