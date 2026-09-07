package org.example.certificatemanagesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.certificatemanagesystem.entity.Admin;

@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
    Admin selectByUsername(@Param("username") String username);
}
