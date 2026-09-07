package org.example.certificatemanagesystem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.certificatemanagesystem.entity.OperationLog;
import org.example.certificatemanagesystem.mapper.OperationLogMapper;
import org.example.certificatemanagesystem.service.OperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {
}
