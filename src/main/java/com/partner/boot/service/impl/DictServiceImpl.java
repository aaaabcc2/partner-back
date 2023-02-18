package com.partner.boot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.partner.boot.entity.Dict;
import com.partner.boot.mapper.DictMapper;
import com.partner.boot.service.IDictService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 啊啊啊啊不吵吵
 * @since 2023-02-17
 */
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements IDictService {

    @Override
    @Cacheable(value="findIcons")
    public List<Dict> findIcons() {
        return list(new QueryWrapper<Dict>().eq("type", "icon"));
    }
}