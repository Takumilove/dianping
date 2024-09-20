package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.SystemConstants;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL,
                TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        // Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L,
        // TimeUnit.SECONDS);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }
        // 7.返回数据
        return Result.ok(shop);

    }

    // 这里需要声明一个线程池，因为下面我们需要新建一个现成来完成重构缓存
    /*
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    */

    /*    public Shop queryWithLogicalExpire(Long id) {
        // 1. 从redis中查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        // 2. 如果未命中，则返回空
        if (StrUtil.isBlank(json)) {
            return null;
        }
        // 3. 命中，将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        // 3.1 将data转为Shop对象
        JSONObject shopJson = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(shopJson, Shop.class);
        // 3.2 获取过期时间
        LocalDateTime expireTime = redisData.getExpireTime();
        // 4. 判断是否过期
        if (LocalDateTime.now().isBefore(expireTime)) {
            // 5. 未过期，直接返回商铺信息
            return shop;
        }
        // 6. 过期，尝试获取互斥锁
        boolean flag = tryLock(LOCK_SHOP_KEY + id);
        // 7. 获取到了锁
        if (flag) {
            // 8. 开启独立线程
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShop2Redis(id, LOCK_SHOP_TTL);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unlock(LOCK_SHOP_KEY + id);
                }
            });
            // 9. 直接返回商铺信息
            return shop;
        }
        // 10. 未获取到锁，直接返回商铺信息
        return shop;
    }*/

    /*    public Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }
    
        // 4.开始实现缓存重建
        // 4.1获取互斥锁
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2判断是否获取成功
            if (!isLock) {
                // 4.3失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
    
            // 4.4成功，根据id查询数据库
            shop = getById(id);
            // 5.不存在，返回错误
            if (shop == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
    
            // 6.存在，将数据写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop),
                    CACHE_SHOP_TTL + ThreadLocalRandom.current().nextInt(-5, 6), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7.释放互斥锁
            unlock(lockKey);
        }
    
        // 8.返回
        return shop;
    }*/

    /*
    public Shop queryWithPassThrough(Long id) {
        // 解决缓存雪崩
        long ttl = CACHE_SHOP_TTL + ThreadLocalRandom.current().nextInt(-5, 6);
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        // 判断命中的是否是空值
        if (shopJson != null) {
            return null;
        }
    
        // 4.不存在，根据id查询数据库
        Shop shop = getById(id);
        // 5.不存在，返回错误
        if (shop == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
    
        // 6.存在，将数据写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), ttl, TimeUnit.MINUTES);
    
        // 7.返回数据
        return shop;
    }
    */

    /*    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }*/

    /*    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }*/

    /*    public void saveShop2Redis(Long id, Long expireSeconds) {
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }*/

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 根据类型分页查询
            Page<Shop> page = query().eq("type_id", typeId)
                                     .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }
        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                                                                                      .search(key,
                                                                                              GeoReference.fromCoordinate(x, y), new Distance(5000), RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                                                                                                                                                                                          .includeDistance()
                                                                                                                                                                                          .limit(end));
        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页
            return Result.ok(Collections.emptyList());
        }
        // 4.1截取from-end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream()
            .skip(from)
            .forEach(result -> {
                // 4.2获取id
                String shopIdStr = result.getContent()
                                         .getName();
                ids.add(Long.valueOf(shopIdStr));
                // 4.3获取距离
                Distance distance = result.getDistance();
                distanceMap.put(shopIdStr, distance);
            });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids)
                                  .last("ORDER BY FIELD(id," + idStr + ")")
                                  .list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId()
                                                 .toString())
                                        .getValue());
        }
        // 6.返回数据
        return Result.ok(shops);
    }
}
