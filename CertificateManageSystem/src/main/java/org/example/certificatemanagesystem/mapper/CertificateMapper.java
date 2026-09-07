package org.example.certificatemanagesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.certificatemanagesystem.entity.Certificate;

import java.util.List;

@Mapper
public interface CertificateMapper extends BaseMapper<Certificate> {

    /** 主页-置顶推荐：仅公开奖状，按置顶权重倒序 */
    List<Certificate> selectPinnedForHome(@Param("limit") int limit);

    /** 主页-按等级排序前三名（排除已置顶的，FIELD 自定义等级次序） */
    List<Certificate> selectTopByLevel(@Param("limit") int limit);

    /** 主页-按获奖时间排序前三名（排除已置顶的） */
    List<Certificate> selectTopByTime(@Param("limit") int limit);
}
