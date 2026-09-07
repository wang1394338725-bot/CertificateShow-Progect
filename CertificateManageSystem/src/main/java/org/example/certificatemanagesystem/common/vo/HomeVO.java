package org.example.certificatemanagesystem.common.vo;

import lombok.Data;

import java.util.List;

/** 主页展示数据：三个区块各一份列表（仅 status=0 的公开奖状） */
@Data
public class HomeVO {
    /** 置顶推荐（is_pinned=1，按 sort_order 权重倒序） */
    private List<CertificateVO> pinned;

    /** 按等级排序前三名（国家级>省级>市级>校级>其他，排除已置顶的） */
    private List<CertificateVO> byLevel;

    /** 按获奖时间排序前三名（排除已置顶的） */
    private List<CertificateVO> byTime;
}
