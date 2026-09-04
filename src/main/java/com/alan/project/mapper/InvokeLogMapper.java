package com.alan.project.mapper;

import com.alan.project.model.entity.InvokeLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 接口调用日志数据库操作
 *
 * @author alan
 */
public interface InvokeLogMapper extends BaseMapper<InvokeLog> {

    /**
     * 按天统计调用次数（趋势图用）
     *
     * @param since 起始日期（含），格式 yyyy-MM-dd
     * @return [{date: yyyy-MM-dd, count: 次数}]，仅包含有调用的日期
     */
    @Select("SELECT DATE_FORMAT(createTime, '%Y-%m-%d') AS `date`, COUNT(*) AS `count` "
            + "FROM invoke_log WHERE isDelete = 0 AND createTime >= #{since} "
            + "GROUP BY DATE_FORMAT(createTime, '%Y-%m-%d') ORDER BY `date` ASC")
    List<Map<String, Object>> listInvokeTrend(@Param("since") String since);

    /**
     * 调用总览：总次数、成功次数、平均耗时、调用用户数
     *
     * @return {totalInvokeNum, successNum, avgCostTime, userNum}
     */
    @Select("SELECT COUNT(*) AS totalInvokeNum, "
            + "IFNULL(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS successNum, "
            + "IFNULL(ROUND(AVG(costTime)), 0) AS avgCostTime, "
            + "COUNT(DISTINCT userId) AS userNum "
            + "FROM invoke_log WHERE isDelete = 0")
    Map<String, Object> getInvokeOverview();
}
