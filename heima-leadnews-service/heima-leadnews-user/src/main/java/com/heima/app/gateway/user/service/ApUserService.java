package com.heima.app.gateway.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.app.gateway.model.common.dtos.ResponseResult;
import com.heima.app.gateway.model.user.dtos.LoginDto;
import com.heima.app.gateway.model.user.pojos.ApUser;

public interface ApUserService extends IService<ApUser> {

    /**
     * app端登录功能
     * @param dto
     * @return
     */
    public ResponseResult login(LoginDto dto);
}
