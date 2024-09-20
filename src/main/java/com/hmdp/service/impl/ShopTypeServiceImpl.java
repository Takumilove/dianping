package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String key = "shop_type_list";
        // 1.查询redis
        List<String> typeJson = stringRedisTemplate.opsForList().range(key, 0, -1);
        // 2.判断是否存在
        if (CollectionUtil.isNotEmpty(typeJson)) {
            // 2.0 如果为空对象(防止缓存穿透时存入的空对象)
            if (StrUtil.isBlank(typeJson.get(0))) {
                return Result.fail("店铺分类信息为空！");
            }
            // 3.如果存在，转换成ShopType类型返回
            List<ShopType> typeList = new ArrayList<>();
            for (String jsonString : typeJson) {
                ShopType shopType = JSONUtil.toBean(jsonString, ShopType.class);
                typeList.add(shopType);
            }
            return Result.ok(typeList);
        }
        // 4.如果不存在，查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 5.数据库也不存在
        if (CollectionUtil.isEmpty(typeList)) {
            // 添加空对象到redis，解决缓存穿透
            stringRedisTemplate.opsForList().rightPushAll(key, CollectionUtil.newArrayList(""));
            stringRedisTemplate.expire(key, CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误
            return Result.fail("店铺分类信息为空！");
        }
        // 5.1数据库存在，存入redis
        List<String> shopTypeList = new ArrayList<>();
        for (ShopType shopType : typeList) {
            shopTypeList.add(JSONUtil.toJsonStr(shopType));
        }
        stringRedisTemplate.opsForList().rightPushAll(key, shopTypeList);
        // 6.返回
        return Result.ok(typeList);
    }
}
