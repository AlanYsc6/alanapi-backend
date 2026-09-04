package com.alan.project.mapper;

import com.alan.project.model.entity.InterfaceInfo;
import com.alan.project.model.vo.InterfaceInfoVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author 19011
* @description 针对表【interface_info(接口信息)】的数据库操作Mapper
* @createDate 2026-08-30 00:39:44
* @Entity com.alan.project.model.entity.InterfaceInfo
*/
public interface InterfaceInfoMapper extends BaseMapper<InterfaceInfo> {

    /**
     * 按总调用次数取接口排行（跨用户汇总 user_interface_info）
     *
     * @param limit 条数
     * @return 接口名称 + 总调用次数，降序
     */
    @Select("SELECT i.id AS id, i.name AS name, SUM(u.totalNum) AS totalNum "
            + "FROM user_interface_info u "
            + "JOIN interface_info i ON u.interfaceInfoId = i.id AND i.isDelete = 0 "
            + "WHERE u.isDelete = 0 "
            + "GROUP BY u.interfaceInfoId, i.id, i.name "
            + "ORDER BY totalNum DESC LIMIT #{limit}")
    List<InterfaceInfoVO> listTopInvokeInterfaceInfo(@Param("limit") int limit);
}
