package org.example.certificatemanagesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.certificatemanagesystem.entity.OperationLog;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
