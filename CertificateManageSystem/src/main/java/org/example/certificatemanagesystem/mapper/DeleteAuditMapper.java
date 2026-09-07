package org.example.certificatemanagesystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.example.certificatemanagesystem.common.vo.AuditMsgVO;
import org.example.certificatemanagesystem.entity.DeleteAudit;

@Mapper
public interface DeleteAuditMapper extends BaseMapper<DeleteAudit> {

    /**
     * 分页查询审核消息（join 管理员表取申请人/审核人姓名）
     * 传入 Page 参数即可被 MyBatis-Plus 分页插件自动拦截分页
     */
    Page<AuditMsgVO> selectAuditMessages(Page<AuditMsgVO> page);
}
