package org.example.certificatemanagesystem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.certificatemanagesystem.common.dto.CertificateQueryDTO;
import org.example.certificatemanagesystem.common.dto.CertificateUpdateDTO;
import org.example.certificatemanagesystem.common.dto.PageResult;
import org.example.certificatemanagesystem.common.dto.PinDTO;
import org.example.certificatemanagesystem.common.dto.UploadDTO;
import org.example.certificatemanagesystem.common.vo.CertificateVO;
import org.example.certificatemanagesystem.common.vo.HomeVO;
import org.example.certificatemanagesystem.common.vo.ImportResultVO;
import org.example.certificatemanagesystem.entity.Certificate;
import org.springframework.web.multipart.MultipartFile;

public interface CertificateService extends IService<Certificate> {
    void uploadCertificate(UploadDTO uploadDTO, MultipartFile file);

    /** Excel 批量导入：逐行校验入库，返回成功/失败明细 */
    ImportResultVO importExcel(MultipartFile file);

    PageResult<CertificateVO> queryCertificatePage(CertificateQueryDTO certificateQueryDTO);

    /** 主页展示：置顶推荐 + 按等级前三 + 按时间前三（仅公开奖状） */
    HomeVO homeShowcase();

    void updateCertificate(CertificateUpdateDTO certificateUpdateDTO);

    /** 更换/补传奖状图片：保存新图、更新 imageUrl、删除旧图文件 */
    void updateImage(Long id, MultipartFile file);

    void updatePinStatus(PinDTO pinDTO);
}
