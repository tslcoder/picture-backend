package com.tanglinlin.picture.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tanglinlin.picture.backend.annotation.AuthCheck;
import com.tanglinlin.picture.backend.common.BaseResponse;
import com.tanglinlin.picture.backend.common.DeleteRequest;
import com.tanglinlin.picture.backend.common.ResultUtils;
import com.tanglinlin.picture.backend.constant.UserConstant;
import com.tanglinlin.picture.backend.exception.BusinessException;
import com.tanglinlin.picture.backend.exception.ErrorCode;
import com.tanglinlin.picture.backend.exception.ThrowUtils;

import com.tanglinlin.picture.backend.model.dto.user.*;
import com.tanglinlin.picture.backend.model.vo.LoginUserVO;
import com.tanglinlin.picture.backend.model.vo.UserVO;
import com.tanglinlin.picture.backend.generator.domain.User;
import com.tanglinlin.picture.backend.generator.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @program: tanglin-picture-backend
 * @ClassName UserController
 * @description:
 * @author: TSL
 * @create: 2025-08-30 23:08
 * @Version 1.0
 **/
@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "使用 JSON 请求体提交注册信息")
    public BaseResponse<?> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用 JSON 请求体提交登录信息")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    @Operation(summary = "获取当前用户登录", description = "从会话中拿")
    public BaseResponse<LoginUserVO> getUserLogin(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出登录")
    public BaseResponse<?> userLogout(HttpServletRequest request) {
       ThrowUtils.throwIf(request==null,ErrorCode.PARAMS_ERROR);
        boolean userLogout = userService.userLogout(request);
        return ResultUtils.success(userLogout);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "创建用户", description = "使用 JSON 请求体提交注册信息")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @Operation(summary = "根据id获取用户", description = "仅管理员")
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @Operation(summary = "根据id获取包装类", description = "仅管理员")
    public BaseResponse<?> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @Operation(summary = "删除用户", description = "仅管理员")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @Operation(summary = "更新用户", description = "仅管理员")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取用户封装列表", description = "仅管理员")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }



}
