package com.partner.boot.service;

import com.partner.boot.controller.domain.LoginDTO;
import com.partner.boot.controller.domain.UserRequest;
import com.partner.boot.entity.Permission;
import com.partner.boot.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 啊啊啊啊不吵吵
 * @since 2023-01-12
 */
public interface IUserService extends IService<User> {

    LoginDTO login(UserRequest user);

    void register(UserRequest user);

    void sendEmail(String email, String type);

    String passwordReset(UserRequest userRequest);

    void logout(String uid);

    User saveUser(User user);

    List<Permission> getPermissions(String roleFlag);

    void passwordChange(UserRequest userRequest);
}
