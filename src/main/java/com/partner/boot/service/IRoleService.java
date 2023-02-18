package com.partner.boot.service;

import com.partner.boot.entity.Role;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 啊啊啊啊不吵吵
 * @since 2023-02-15
 */
public interface IRoleService extends IService<Role> {

    void savePermissions(Integer roleId, List<Integer> permissionIds);

}
