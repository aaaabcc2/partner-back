package com.partner.boot.service.impl;

import com.partner.boot.entity.Permission;
import com.partner.boot.mapper.PermissionMapper;
import com.partner.boot.service.IPermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 啊啊啊啊不吵吵
 * @since 2023-02-15
 */
@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements IPermissionService {

    @Override
    public List<Permission> tree() {
        List<Permission> allData = list();

        return childrenTree(null, allData); // 从第一级开始往下递归获取树
    }

    // 递归生成树
    private List<Permission> childrenTree(Integer pid, List<Permission> allData) {
        List<Permission> list = new ArrayList<>();
        for (Permission permission : allData) {
            if (Objects.equals(permission.getPid(), pid)) {  // null, 一级
                list.add(permission);
                List<Permission> childrenTree = childrenTree(permission.getId(), allData);  // 递归调用， 摘取二级节点、三级、四级...
                permission.setChildren(childrenTree);
            }
        }
        return list;
    }
}