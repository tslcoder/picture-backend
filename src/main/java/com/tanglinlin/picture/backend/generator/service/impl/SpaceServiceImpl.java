package com.tanglinlin.picture.backend.generator.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import com.tanglinlin.picture.backend.exception.ThrowUtils;
import com.tanglinlin.picture.backend.generator.domain.Space;
import com.tanglinlin.picture.backend.generator.service.SpaceService;
import com.tanglinlin.picture.backend.generator.mapper.SpaceMapper;
import com.tanglinlin.picture.backend.model.enums.SpaceLevelEnum;
import org.springframework.stereotype.Service;

/**
 * @author tangshilin
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-09-09 14:17:41
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        //从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        //要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                ThrowUtils.throwIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR,
                        "空间名字不能为空");
            }
            if (spaceLevel == null) {
                ThrowUtils.throwIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR,
                        "空间等级不能为空");
            }
        }

        //修改数据时，要修改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            ThrowUtils.throwIf(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR,
                    "空间级别不正确");
        }
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 20) {
            ThrowUtils.throwIf(spaceName.length() > 20, ErrorCode.PARAMS_ERROR, "空间名字过长");
        }
    }
}




