package com.tanglinlin.picture.backend.generator.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tanglinlin.picture.backend.generator.domain.Picture;
import com.tanglinlin.picture.backend.generator.domain.User;
import com.tanglinlin.picture.backend.model.dto.picture.PictureQueryRequest;
import com.tanglinlin.picture.backend.model.dto.picture.PictureReviewRequest;
import com.tanglinlin.picture.backend.model.dto.picture.PictureUpdateRequest;
import com.tanglinlin.picture.backend.model.dto.picture.PictureUploadRequest;
import com.tanglinlin.picture.backend.model.enums.PictureReviewStatusEnum;
import com.tanglinlin.picture.backend.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
* @author tangshilin
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-09-01 09:56:33
*/
public interface PictureService extends IService<Picture> {

    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    void validPicture(Picture picture);

    PictureVO getPictureVO(Picture picture, HttpServletRequest httpServletRequest);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest httpServletRequest);

    void doPictureReview(PictureReviewRequest pictureReviewRequest,User loginUser);

    void fillReviewParams(Picture picture,User loginUser);
}
