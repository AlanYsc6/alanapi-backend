package com.alan.project.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 接口调用日志
 * @TableName invoke_log
 */
@TableName(value = "invoke_log")
@Data
public class InvokeLog implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 调用用户 id
     */
    private Long userId;

    /**
     * 接口 id（0-平台未登记的接口）
     */
    private Long interfaceInfoId;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方式
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应数据
     */
    private String responseBody;

    /**
     * 调用状态（0-失败，1-成功）
     */
    private Integer status;

    /**
     * 耗时（毫秒）
     */
    private Long costTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除(0-未删, 1-已删)
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 用户名称（列表展示用，非表字段）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 接口名称（列表展示用，非表字段）
     */
    @TableField(exist = false)
    private String interfaceName;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
