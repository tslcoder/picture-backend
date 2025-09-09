package com.tanglinlin.picture.backend.generator.service;

import com.tanglinlin.picture.backend.generator.domain.Space;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author tangshilin
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-09-09 14:17:41
*/
public interface SpaceService extends IService<Space> {
    void validSpace(Space space,boolean add);

}
