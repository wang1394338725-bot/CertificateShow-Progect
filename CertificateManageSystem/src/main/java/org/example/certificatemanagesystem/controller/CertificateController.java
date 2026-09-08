package org.example.certificatemanagesystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.certificatemanagesystem.common.dto.CertificateQueryDTO;
import org.example.certificatemanagesystem.common.dto.CertificateUpdateDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.dto.PinDTO;
import org.example.certificatemanagesystem.common.dto.UploadDTO;
import org.example.certificatemanagesystem.common.vo.CertificateVO;
import org.example.certificatemanagesystem.common.vo.HomeVO;
import org.example.certificatemanagesystem.common.vo.ImportResultVO;
import org.example.certificatemanagesystem.common.vo.ResultVO;
import org.example.certificatemanagesystem.config.AuthInterceptor;
import org.example.certificatemanagesystem.service.CertificateService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService certificateService;

    @PostMapping("/upload")
    public ResultVO<String> upload(@ModelAttribute UploadDTO uploadDTO,
            @RequestParam("file") MultipartFile file) {
        certificateService.uploadCertificate(uploadDTO, file);
        return ResultVO.success();
    }

    /** Excel 批量导入（需登录，拦截器保证）：逐行校验，单行失败不影响其余行 */
    @PostMapping("/import")
    public ResultVO<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        return ResultVO.success(certificateService.importExcel(file));
    }

    /** 主页展示（公开）：置顶推荐 + 按等级前三 + 按时间前三，仅含 status=0 的公开奖状 */
    @PostMapping("/home")
    public ResultVO<HomeVO> home() {
        return ResultVO.success(certificateService.homeShowcase());
    }

    /** 主页搜索（公开）：与 /view 同样的检索能力，但强制只返回 status=0 的公开奖状 */
    @PostMapping("/search")
    public ResultVO<PageResult<CertificateVO>> search(@RequestBody CertificateQueryDTO certificateQueryDTO) {
        certificateQueryDTO.setStatus("0");
        return ResultVO.success(certificateService.queryCertificatePage(certificateQueryDTO));
    }

    @PostMapping("/view")
    public ResultVO<PageResult<CertificateVO>> view(@RequestBody CertificateQueryDTO certificateQueryDTO,
            HttpServletRequest request) {
        // 高危漏洞修复：/view 是公开接口，未登录访客强制只看正常状态奖状（防止 curl 拖库）；
        // 已登录管理员可查全部状态（待审核删除/已隐藏）
        if (request.getAttribute(AuthInterceptor.USER_ID_ATTR) == null) {
            certificateQueryDTO.setStatus("0");
        }
        return ResultVO.success(certificateService.queryCertificatePage(certificateQueryDTO));
    }

    /** 导出全部正常奖状为 Excel（需登录，拦截器保证）：基本信息 + 图片地址列，表头与导入兼容 */
    @GetMapping("/export")
    public void exportExcel(HttpServletRequest request, HttpServletResponse response) {
        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        certificateService.exportExcel(baseUrl, response);
    }

    @PutMapping("/update")
    public ResultVO<String> update(@RequestBody CertificateUpdateDTO certificateUpdateDTO) {
        certificateService.updateCertificate(certificateUpdateDTO);
        return ResultVO.success("更新成功");
    }

    /** 更换/补传奖状图片（需登录，拦截器保证）：id + file，Excel 导入的无图奖状也走此接口补图 */
    @PutMapping("/update-image")
    public ResultVO<String> updateImage(@RequestParam("id") Long id,
            @RequestParam("file") MultipartFile file) {
        certificateService.updateImage(id, file);
        return ResultVO.success("图片已更新");
    }

    @PutMapping("/pin")
    public ResultVO<String> pin(@RequestBody PinDTO pinDTO) {
        certificateService.updatePinStatus(pinDTO);
        return ResultVO.success();
    }
}
