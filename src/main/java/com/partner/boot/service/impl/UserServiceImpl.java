package com.partner.boot.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.partner.boot.common.Constants;
import com.partner.boot.common.enums.EmailCodeEnum;
import com.partner.boot.controller.domain.LoginDTO;
import com.partner.boot.controller.domain.UserRequest;
import com.partner.boot.entity.Permission;
import com.partner.boot.entity.Role;
import com.partner.boot.entity.RolePermission;
import com.partner.boot.entity.User;
import com.partner.boot.exception.ServiceException;
import com.partner.boot.mapper.RolePermissionMapper;
import com.partner.boot.mapper.UserMapper;
import com.partner.boot.service.IPermissionService;
import com.partner.boot.service.IRoleService;
import com.partner.boot.service.IUserService;
import com.partner.boot.utils.EmailUtils;
import com.partner.boot.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 啊啊啊啊不吵吵
 * @since 2023-01-12
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final long TIME_IN_MS5 = 5 * 60 * 1000;  // 表示5分钟的毫秒数

    @Autowired
    EmailUtils emailUtils;

    @Resource
    RolePermissionMapper rolePermissionMapper;

    @Resource
    IRoleService roleService;

    @Resource
    IPermissionService permissionService;

    @Override
    public LoginDTO login(UserRequest user) {
        User dbUser;
        try {
            dbUser = getOne(new UpdateWrapper<User>().eq("username", user.getUsername())
                    .or().eq("email", user.getUsername()));
        } catch (Exception e) {
            throw new RuntimeException("数据库异常");
        }
        if (dbUser == null) {
            throw new ServiceException("未找到用户");
        }
//        String securePass = SaSecureUtil.aesEncrypt(Constants.LOGIN_USER_KEY, user.getPassword());
//        if (!securePass.equals(dbUser.getPassword())) {
//            throw new ServiceException("用户名或密码错误");
//        }
        if (!BCrypt.checkpw(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        // 登录
        StpUtil.login(dbUser.getUid());  // loginId
        StpUtil.getSession().set(Constants.LOGIN_USER_KEY, dbUser);
        String tokenValue = StpUtil.getTokenInfo().getTokenValue();
//        LoginDTO loginDTO = new LoginDTO(dbUser, tokenValue);

        // 查询用户的菜单树（2层）
        String flag = dbUser.getRole();
        List<Permission> all = getPermissions(flag);  // 水平
        List<Permission> menus = getTreePermissions(all); // 树
        // 页面的按钮权限集合
        List<Permission> auths = all.stream().filter(permission -> permission.getType() == 3).collect(Collectors.toList());
        return LoginDTO.builder().user(dbUser).token(tokenValue).menus(menus).auths(auths).build();
    }

    public List<Permission> getPermissions(String roleFlag) {
        Role role = roleService.getOne(new QueryWrapper<Role>().eq("flag", roleFlag));
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(new QueryWrapper<RolePermission>().eq("role_id", role.getId()));
        List<Integer> permissionIds = rolePermissions.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
        List<Permission> permissionList = permissionService.list();
        List<Permission> all = new ArrayList<>();  // 水平的菜单树
        for (Integer permissionId : permissionIds) {
            permissionList.stream().filter(permission -> permission.getId().equals(permissionId)).findFirst()
                    .ifPresent(all::add);
        }
        return all;
    }

    public void passwordChange(UserRequest userRequest) {
        User dbUser = getOne(new UpdateWrapper<User>().eq("uid", userRequest.getUid()));
        if (dbUser == null) {
            throw new ServiceException("未找到用户");
        }
        boolean checkpw = BCrypt.checkpw(userRequest.getPassword(), dbUser.getPassword());
        if (!checkpw) {
            throw new ServiceException("原密码错误");
        }
        String newPass = userRequest.getNewPassword();
        dbUser.setPassword(BCrypt.hashpw(newPass));
        updateById(dbUser);   // 设置到数据库
    }

    // 获取角色对应的菜单树
    private List<Permission> getTreePermissions(List<Permission> all) {
        // 菜单树 1级 -> 2级
        List<Permission> parentList = all.stream().filter(permission -> permission.getType() == 1
                || (permission.getType() == 2 && permission.getPid() == null)).collect(Collectors.toList());// type==1 是目录  或者  pid = null
        for (Permission permission : parentList) {
            Integer pid = permission.getId();
            List<Permission> level2List = all.stream().filter(permission1 -> pid.equals(permission1.getPid())).collect(Collectors.toList());// 2级菜单
            permission.setChildren(level2List);
        }
        return parentList.stream().sorted(Comparator.comparing(Permission::getOrders)).collect(Collectors.toList());  // 排序
    }

    @Override
    public void register(UserRequest user) {
        // 校验邮箱
        String key = Constants.EMAIL_CODE + EmailCodeEnum.REGISTER.getValue() + user.getEmail();
        validateEmail(key, user.getEmailCode());
        try {
            User saveUser = new User();
            BeanUtils.copyProperties(user, saveUser);   // 把请求数据的属性copy给存储数据库的属性
            // 存储用户信息
            saveUser(saveUser);
        } catch (Exception e) {
            throw new RuntimeException("数据库异常", e);
        }
    }

    @Override
    public void sendEmail(String email, String type) {
        String emailPrefix = EmailCodeEnum.getValue(type);
        if (StrUtil.isBlank(emailPrefix)) {
            throw new ServiceException("不支持的邮箱验证类型");
        }
        // 设置redis key
        String key = Constants.EMAIL_CODE + emailPrefix + email;
        Long expireTime = RedisUtils.getExpireTime(key);
        // 限制超过1分钟才可以继续发送邮件，判断过期时间是否大于4分钟
        if (expireTime != null && expireTime > 4 * 60) {
            throw new ServiceException("发送邮箱验证过于频繁");
        }

        Integer code = Integer.valueOf(RandomUtil.randomNumbers(6));
        log.info("本次验证的code是：{}", code);
        String context = "<b>尊敬的用户：</b><br><br><br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;您好，" +
                "Partner交友网提醒您本次的验证码是：<b>{}</b>，" +
                "有效期5分钟。<br><br><br><b>Partner交友网</b>";
        String html = StrUtil.format(context, code);
        // 校验邮箱是否已注册
        User user = getOne(new QueryWrapper<User>().eq("email", email));
        if (EmailCodeEnum.REGISTER.equals(EmailCodeEnum.getEnum(type))) {  // 无需权限验证即可发送邮箱验证码
            if (user != null) {
                throw new ServiceException("邮箱已注册");
            }
        } else if (EmailCodeEnum.RESET_PASSWORD.equals(EmailCodeEnum.getEnum(type))) {
            if (user == null) {
                throw new ServiceException("未找到用户");
            }
        }
        // 忘记密码
        ThreadUtil.execAsync(() -> {   // 多线程执行异步请求，可以防止网络阻塞
            emailUtils.sendHtml("【Partner交友网】验证提醒", html, email);

            RedisUtils.setCacheObject(key, code, TIME_IN_MS5, TimeUnit.MILLISECONDS);
        });

    }

    /**
     * 重置密码
     *
     * @param userRequest
     * @return
     */
    @Override
    public String passwordReset(UserRequest userRequest) {
        String email = userRequest.getEmail();
        User dbUser = getOne(new UpdateWrapper<User>().eq("email", email));
        if (dbUser == null) {
            throw new ServiceException("未找到用户");
        }
        // 校验邮箱验证码
        String key = Constants.EMAIL_CODE + EmailCodeEnum.RESET_PASSWORD.getValue() + email;
        validateEmail(key, userRequest.getEmailCode());
        String newPass = "123";
        dbUser.setPassword(BCrypt.hashpw(newPass));
        try {
            updateById(dbUser);   // 设置到数据库
        } catch (Exception e) {
            throw new RuntimeException("注册失败", e);
        }
        return newPass;
    }

    @Override
    public void logout(String uid) {
        // 退出登录
        StpUtil.logout(uid);
        log.info("用户{}退出成功", uid);
    }

    /**
     * 校验邮箱
     *
     * @param emailCode
     */
    private void validateEmail(String emailKey, String emailCode) {
        // 校验邮箱
        Integer code = RedisUtils.getCacheObject(emailKey);
        if (code == null) {
            throw new ServiceException("验证码失效");
        }
        if (!emailCode.equals(code.toString())) {
            throw new ServiceException("验证码错误");
        }
        RedisUtils.deleteObject(emailKey);  // 清除缓存
    }


    public User saveUser(User user) {
        User dbUser = getOne(new UpdateWrapper<User>().eq("username", user.getUsername()));
        if (dbUser != null) {
            throw new ServiceException("用户已存在");
        }
        // 设置昵称
        if (StrUtil.isBlank(user.getName())) {
//                String name = Constants.USER_NAME_PREFIX + DateUtil.format(new Date(), Constants.DATE_RULE_YYYYMMDD)
//                        + RandomUtil.randomString(4);
            user.setName("系统用户" + RandomUtil.randomString(6));
        }
        if (StrUtil.isBlank(user.getPassword())) {
            user.setPassword("123");   // 设置默认密码
        }
        // 加密用户密码
        user.setPassword(BCrypt.hashpw(user.getPassword()));  // BCrypt加密
        // 设置唯一标识
        user.setUid(IdUtil.fastSimpleUUID());
        try {
            save(user);
        } catch (Exception e) {
            throw new RuntimeException("注册失败", e);
        }
        return user;
    }

}