package org.example.certificatemanagesystem.common.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Excel 批量导入结果：成功/失败条数 + 逐行失败原因 */
@Data
public class ImportResultVO {
    private int total;
    private int successCount;
    private int failCount;
    private List<String> failures = new ArrayList<>();
}
