package org.example.certificatemanagesystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.common.dto.CertificateQueryDTO;
import org.example.certificatemanagesystem.common.dto.CertificateUpdateDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.dto.PinDTO;
import org.example.certificatemanagesystem.common.dto.UploadDTO;
import org.example.certificatemanagesystem.common.exception.BusinessException;
import org.example.certificatemanagesystem.common.vo.CertificateVO;
import org.example.certificatemanagesystem.common.vo.HomeVO;
import org.example.certificatemanagesystem.common.vo.ImportResultVO;
import org.example.certificatemanagesystem.entity.Certificate;
import org.example.certificatemanagesystem.mapper.CertificateMapper;
import org.example.certificatemanagesystem.service.CertificateService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CertificateServiceImpl extends ServiceImpl<CertificateMapper, Certificate> implements CertificateService {
    private final CertificateMapper certificateMapper;

    // 上传根目录（yml: upload.base-path），仅保留 /uploads 前缀写入 imageUrl
    @Value("${upload.base-path}")
    private String uploadBasePath;

    @Override
    public void uploadCertificate(UploadDTO uploadDTO, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        String imageUrl = saveImage(file);

        Certificate certificate = new Certificate();
        BeanUtils.copyProperties(uploadDTO, certificate);
        certificate.setImageUrl(imageUrl);
        certificate.setStatus(0);

        certificateMapper.insert(certificate);
    }

    @Override
    public void updateImage(Long id, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }
        Certificate certificate = certificateMapper.selectById(id);
        if (certificate == null) {
            throw new BusinessException("奖状不存在，ID:" + id);
        }

        String imageUrl = saveImage(file);

        LambdaUpdateWrapper<Certificate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Certificate::getId, id).set(Certificate::getImageUrl, imageUrl);
        certificateMapper.update(null, wrapper);

        deleteLocalImage(certificate.getImageUrl());
    }

    /** 保存图片文件到本地，返回 imageUrl（/uploads/awards/yyyy/MM/dd/uuid.ext） */
    private String saveImage(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        assert originalName != null;
        String ext = originalName.substring((originalName.lastIndexOf(".")));
        String newFileName = UUID.randomUUID().toString() + ext;

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uploadDir = uploadBasePath + "/awards/" + datePath + "/";
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("创建图片存储目录失败: " + uploadDir, e);
        }

        File dest = new File(uploadDir + newFileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败");
        }
        return "/uploads/awards/" + datePath + "/" + newFileName;
    }

    /** 删除旧图片文件（/uploads/... 映射回本地路径），失败静默不影响主流程 */
    private void deleteLocalImage(String imageUrl) {
        if (!StringUtils.hasText(imageUrl) || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            Path oldPath = Paths.get(uploadBasePath, imageUrl.substring("/uploads/".length()));
            Files.deleteIfExists(oldPath);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void exportExcel(String baseUrl, jakarta.servlet.http.HttpServletResponse response) {
        // 导出全部正常状态奖状，按获奖日期倒序；表头与导入别名一致，导出的表格可直接再导入
        List<Certificate> list = certificateMapper.selectList(
                new LambdaQueryWrapper<Certificate>()
                        .eq(Certificate::getStatus, 0)
                        .orderByDesc(Certificate::getAwardDate)
                        .orderByDesc(Certificate::getId));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("奖状列表");

            // 表头样式：加粗 + 灰底
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = { "奖状名称", "获奖成员", "赛事名称", "奖状等级", "所属项目", "获奖日期", "图片地址" };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            sheet.setColumnWidth(6, 46 * 256); // 图片地址列较宽

            int r = 1;
            for (Certificate c : list) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(defaultIfBlank(c.getTitle()));
                row.createCell(1).setCellValue(defaultIfBlank(c.getRecipient()));
                row.createCell(2).setCellValue(defaultIfBlank(c.getEventName()));
                row.createCell(3).setCellValue(defaultIfBlank(c.getAwardLevel()));
                row.createCell(4).setCellValue(defaultIfBlank(c.getProjectName()));
                row.createCell(5).setCellValue(c.getAwardDate() != null ? c.getAwardDate().toString() : "");
                // 相对路径拼成完整 URL，Excel 内可直接点击查看图片
                row.createCell(6).setCellValue(StringUtils.hasText(c.getImageUrl()) ? baseUrl + c.getImageUrl() : "");
            }

            String fileName = URLEncoder.encode(
                    "奖状导出_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx",
                    StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new BusinessException("导出失败：" + e.getMessage());
        }
    }

    private String defaultIfBlank(String s) {
        return StringUtils.hasText(s) ? s : "";
    }

    /** 分页参数防刷钳制：size 上限 50（防 size=10000 一次拖全表），current 上限 1000（防超深 offset 拖垮 DB） */
    private Page<Certificate> clampPage(Integer current, Integer size) {
        int c = current == null ? 1 : Math.min(Math.max(current, 1), 1000);
        int s = size == null ? 12 : Math.min(Math.max(size, 1), 50);
        return new Page<>(c, s);
    }

    @Override
    public PageResult<CertificateVO> queryCertificatePage(CertificateQueryDTO certificateQueryDTO) {
        // 1. 构建 MyBatis-Plus 分页对象（参数经防刷钳制）
        Page<Certificate> page = clampPage(certificateQueryDTO.getCurrent(), certificateQueryDTO.getSize());

        // 2. 构建查询条件（Wrapper）
        LambdaQueryWrapper<Certificate> wrapper = new LambdaQueryWrapper<>();

        // 关键词模糊搜索：匹配 奖状名称(title) 或 获奖人员(recipient) 或 赛事名称(eventName)
        if (StringUtils.hasText(certificateQueryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Certificate::getTitle, certificateQueryDTO.getKeyword())
                    .or()
                    .like(Certificate::getRecipient, certificateQueryDTO.getKeyword())
                    .or()
                    .like(Certificate::getEventName, certificateQueryDTO.getKeyword()));
        }
        // 精确匹配级别
        if (StringUtils.hasText(certificateQueryDTO.getLevel())) {
            wrapper.eq(Certificate::getAwardLevel, certificateQueryDTO.getLevel());
        }
        // 精确匹配状态
        if (StringUtils.hasText(certificateQueryDTO.getStatus())) {
            wrapper.eq(Certificate::getStatus, certificateQueryDTO.getStatus());
        }

        // 3. 排序规则：sortBy 指定时按指定方式，否则置顶优先（置顶 → sort_order 权重倒序 → 创建时间倒序）
        if ("level".equals(certificateQueryDTO.getSortBy())) {
            // 等级按 国家>省>市>校>其他 的业务顺序（FIELD 返回序号），同级内按获奖日期倒序
            wrapper.last("ORDER BY FIELD(award_level, '国家级', '省级', '市级', '校级', '其他'), award_date DESC, id DESC");
        } else if ("time".equals(certificateQueryDTO.getSortBy())) {
            wrapper.orderByDesc(Certificate::getAwardDate)
                    .orderByDesc(Certificate::getId);
        } else {
            wrapper.orderByDesc(Certificate::getIsPinned)
                    .orderByDesc(Certificate::getSortOrder)
                    .orderByDesc(Certificate::getCreateTime);
        }

        // 4. 执行分页查询
        Page<Certificate> certificatePage = certificateMapper.selectPage(page, wrapper);

        // 5. 【关键步骤】Entity 列表 转换为 VO 列表
        List<CertificateVO> voList = certificatePage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        // 6. 组装分页结果
        PageResult<CertificateVO> pageResult = new PageResult<>();
        pageResult.setRecords(voList);
        pageResult.setTotal(certificatePage.getTotal());
        pageResult.setCurrent((int) certificatePage.getCurrent());
        pageResult.setSize((int) certificatePage.getSize());

        return pageResult;
    }

    @Override
    public HomeVO homeShowcase() {
        // 三个区块各自限量：置顶最多 6 条，等级/时间各取前三名（SQL 内已排除置顶避免重复展示）
        HomeVO homeVO = new HomeVO();
        homeVO.setPinned(
                certificateMapper.selectPinnedForHome(6).stream().map(this::toVO).collect(Collectors.toList()));
        homeVO.setByLevel(certificateMapper.selectTopByLevel(3).stream().map(this::toVO).collect(Collectors.toList()));
        homeVO.setByTime(certificateMapper.selectTopByTime(3).stream().map(this::toVO).collect(Collectors.toList()));
        return homeVO;
    }

    /** Entity 转 VO（同名字段自动拷贝，isPinned 命名一致可直接映射） */
    private CertificateVO toVO(Certificate entity) {
        CertificateVO vo = new CertificateVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    @Override
    public void updateCertificate(CertificateUpdateDTO certificateUpdateDTO) {
        Certificate certificate = certificateMapper.selectById(certificateUpdateDTO.getId());
        if (certificate == null) {
            throw new BusinessException("奖状不存在,id=" + certificateUpdateDTO.getId());
        }

        BeanUtils.copyProperties(certificateUpdateDTO, certificate, "imageUrl");

        certificateMapper.updateById(certificate);
    }

    @Override
    public void updatePinStatus(PinDTO pinDTO) {
        // 先确认奖状存在，避免对不存在的 id 做无意义更新
        Certificate certificate = certificateMapper.selectById(pinDTO.getId());
        if (certificate == null) {
            throw new BusinessException("奖状不存在,id=" + pinDTO.getId());
        }

        // 置顶时分配递增权重（越晚置顶越靠前）；取消置顶时权重归零
        LambdaUpdateWrapper<Certificate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Certificate::getId, pinDTO.getId())
                .set(Certificate::getIsPinned, pinDTO.getIsPinned())
                .set(Certificate::getSortOrder, pinDTO.getIsPinned() ? nextSortOrder() : 0);

        certificateMapper.update(null, wrapper);
    }

    /**
     * 查询当前最大的置顶权重并 +1
     */
    private int nextSortOrder() {
        Certificate top = certificateMapper.selectOne(new LambdaQueryWrapper<Certificate>()
                .select(Certificate::getSortOrder)
                .orderByDesc(Certificate::getSortOrder)
                .last("LIMIT 1"));
        return top == null ? 1 : top.getSortOrder() + 1;
    }

    // ==================== Excel 批量导入 ====================

    /** 表头别名 → 字段（列顺序不限，学长表格的常见写法都能识别） */
    private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
            "title", List.of("奖状名称", "名称", "标题", "奖项名称"),
            "recipient", List.of("获奖成员", "获奖人员", "成员", "人员", "姓名", "获奖者"),
            "eventName", List.of("赛事名称", "赛事", "比赛名称", "比赛", "竞赛名称", "竞赛"),
            "awardLevel", List.of("奖状等级", "获奖等级", "获奖级别", "等级", "级别"),
            "projectName", List.of("所属项目", "项目名称", "获奖项目", "项目"),
            "awardDate", List.of("获奖日期", "日期", "获奖时间", "时间"));

    @Override
    public ImportResultVO importExcel(MultipartFile file) {
        ImportResultVO result = new ImportResultVO();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new BusinessException("表格为空");
            }

            // 表头 → 列号 映射（包含式别名匹配）
            Map<String, Integer> colMap = new HashMap<>();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                final int col = c; // lambda 只能捕获 effectively final 变量
                String name = readString(header.getCell(col)).replace(" ", "");
                if (name.isBlank())
                    continue;
                HEADER_ALIASES.forEach((field, aliases) -> {
                    if (!colMap.containsKey(field) && aliases.stream().anyMatch(name::contains)) {
                        colMap.put(field, col);
                    }
                });
            }
            if (!colMap.containsKey("title")) {
                throw new BusinessException("缺少必需表头：奖状名称（支持：奖状名称/名称/标题/奖项名称）");
            }

            // 逐行入库：单行失败只记录原因，不中断其余行（手工表格难免脏数据）
            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null)
                    continue;
                String title = readString(cellAt(row, colMap, "title"));
                if (title.isBlank())
                    continue; // 尾部空行直接跳过
                result.setTotal(result.getTotal() + 1);
                try {
                    Certificate cert = new Certificate();
                    cert.setTitle(title);
                    cert.setRecipient(readString(cellAt(row, colMap, "recipient")));
                    cert.setEventName(readString(cellAt(row, colMap, "eventName")));
                    cert.setAwardLevel(normalizeLevel(readString(cellAt(row, colMap, "awardLevel"))));
                    cert.setProjectName(readString(cellAt(row, colMap, "projectName")));
                    cert.setAwardDate(readDate(cellAt(row, colMap, "awardDate")));
                    cert.setStatus(0);
                    certificateMapper.insert(cert);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception e) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getFailures().add("第" + (r + 1) + "行「" + title + "」：" + e.getMessage());
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败，请确认是 .xlsx 或 .xls 文件");
        }
        return result;
    }

    private Cell cellAt(Row row, Map<String, Integer> colMap, String field) {
        Integer idx = colMap.get(field);
        return idx == null ? null : row.getCell(idx);
    }

    /** 单元格统一按文本读取：数字去尾零，日期单元格转 ISO 字符串 */
    private String readString(Cell cell) {
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : (cell.getNumericCellValue() == Math.floor(cell.getNumericCellValue())
                            ? String.valueOf((long) cell.getNumericCellValue())
                            : String.valueOf(cell.getNumericCellValue()));
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /** 等级归一化：含"国家"→国家级，含"省"→省级，含"市"→市级，含"校/院"→校级，其余→其他 */
    private String normalizeLevel(String raw) {
        if (raw.isBlank())
            return null;
        if (raw.contains("国家"))
            return "国家级";
        if (raw.contains("省"))
            return "省级";
        if (raw.contains("市"))
            return "市级";
        if (raw.contains("校") || raw.contains("院"))
            return "校级";
        return "其他";
    }

    /**
     * 日期解析：Excel 日期格式直接取；字符串支持 2024-05-01 / 2024/5/1 / 2024.5.1 / 2024年5月1日 /
     * 2024年5月，解析失败留空不阻断
     */
    private LocalDate readDate(Cell cell) {
        if (cell == null)
            return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = readString(cell)
                .replace("年", "-").replace("月", "-").replace("日", "")
                .replace(".", "-").replace("/", "-")
                .replaceAll("-+$", "");
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-M-d"));
        } catch (DateTimeException ignored) {
            // 继续尝试只到月份的写法
        }
        try {
            return YearMonth.parse(s, DateTimeFormatter.ofPattern("yyyy-M")).atDay(1);
        } catch (DateTimeException ignored) {
            return null;
        }
    }
}
