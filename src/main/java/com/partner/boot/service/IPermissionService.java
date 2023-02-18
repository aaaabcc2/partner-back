package com.partner.boot.service;

import com.partner.boot.common.Result;
import com.partner.boot.entity.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 啊啊啊啊不吵吵
 * @since 2023-02-15
 */
public interface IPermissionService extends IService<Permission> {

    List<Permission> tree();
}
