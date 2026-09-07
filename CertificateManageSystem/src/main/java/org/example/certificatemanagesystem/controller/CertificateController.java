package org.example.certificatemanagesystem.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import org.example.certificatemanagesystem.service.CertificateService;
import org.example.certificatemanagesystem.common.utils.JwtUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
public class CertificateController {
    private final CertificateService certificateService;
    private final JwtUtil jwtUtil;

    private Long parseUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            return Long.parseLong(jwtUtil.getUserIdFromToken(authHeader.substring(7)));
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/upload")
    public ResultVO<String> upload(@ModelAttribute UploadDTO uploadDTO,
            @RequestParam("file") MultipartFile file) {
        certificateService.uploadCertificate(uploadDTO, file);
        return ResultVO.success();
    }

    /** Excel 批量导入（需登录）：逐行校验，单行失败不影响其余行 */
    @PostMapping("/import")
    public ResultVO<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long userId = parseUserId(request);
        if (userId == null) {
            return ResultVO.error(401, "未登录或 token 无效");
        }
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
    public ResultVO<PageResult<CertificateVO>> view(@RequestBody CertificateQueryDTO certificateQueryDTO) {
        PageResult<CertificateVO> pageResult = certificateService.queryCertificatePage(certificateQueryDTO);
        return ResultVO.success(pageResult);
    }

    @PutMapping("/update")
    public ResultVO<String> update(@RequestBody CertificateUpdateDTO certificateUpdateDTO) {
        certificateService.updateCertificate(certificateUpdateDTO);
        return ResultVO.success("更新成功");
    }

    /** 更换/补传奖状图片（需登录）：id + file，Excel 导入的无图奖状也走此接口补图 */
    @PutMapping("/update-image")
    public ResultVO<String> updateImage(@RequestParam("id") Long id,
            @RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = parseUserId(request);
        if (userId == null) {
            return ResultVO.error(401, "未登录或 token 无效");
        }
        certificateService.updateImage(id, file);
        return ResultVO.success("图片已更新");
    }

    @PutMapping("/pin")
    public ResultVO<String> pin(@RequestBody PinDTO pinDTO) {
        certificateService.updatePinStatus(pinDTO);
        return ResultVO.success();
    }
}
