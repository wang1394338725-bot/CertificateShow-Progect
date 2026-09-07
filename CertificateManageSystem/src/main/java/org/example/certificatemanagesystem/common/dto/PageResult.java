package org.example.certificatemanagesystem.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;  // 当前页数据列表
    private Long total;  // 总记录数
    private Integer current; // 当前页码
    private Integer size;  // 每条页数
}