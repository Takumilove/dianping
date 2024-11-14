 package com.hmdp;


 import com.hmdp.entity.Shop;
 import com.hmdp.service.impl.ShopServiceImpl;
 import com.hmdp.utils.CacheClient;
 import com.hmdp.utils.RedisIdWorker;
 import org.junit.jupiter.api.Test;
 import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.data.geo.Point;
 import org.springframework.data.redis.connection.RedisGeoCommands;
 import org.springframework.data.redis.core.StringRedisTemplate;

 import javax.annotation.Resource;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Map;
 import java.util.concurrent.CountDownLatch;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.TimeUnit;
 import java.util.stream.Collectors;

 import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

 @SpringBootTest
 class HmDianPingApplicationTests {

     @Resource
     private CacheClient cacheClient;

     @Resource
     private ShopServiceImpl shopService;

     @Resource
     private RedisIdWorker redisIdWorker;

     @Resource
     private StringRedisTemplate stringRedisTemplate;

     private ExecutorService es = Executors.newFixedThreadPool(500);

//     @Test
     void testIdWorker() throws InterruptedException {
         CountDownLatch latch = new CountDownLatch(300);
         Runnable task = () -> {
             for (int i = 0; i < 100; i++) {
                 long id = redisIdWorker.nextId("order");
                 System.out.println("id = " + id);
             }
             latch.countDown();
         };
         long begin = System.currentTimeMillis();
         for (int i = 0; i < 300; i++) {
             es.submit(task);
         }
         latch.await();
         long end = System.currentTimeMillis();
         System.out.println("耗时：" + (end - begin) + "ms");
     }

     @Test
     public void testSaveShop() {
         Shop shop = shopService.getById(1L);

         cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
     }

     @Test
     void loadShopData() {
         // 1.查询店铺信息
         List<Shop> list = shopService.list();
         // 2.把店铺分组，按照typeId分组，id一致的放到一个集合
         Map<Long, List<Shop>> map = list.stream()
                                         .collect(Collectors.groupingBy(Shop::getTypeId));
         // 3.分批完成写入redis
         for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
             // 获取类型id
             Long typeId = entry.getKey();
             String key = "shop:geo:" + typeId;
             // 获取到同类型的店铺
             List<Shop> value = entry.getValue();
             List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
             for (Shop shop : value) {
                 // stringRedisTemplate.opsForGeo()
                 //                    .add(key, new Point(shop.getX(), shop.getY()), shop.getId()
                 //                                                                       .toString());
                 locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId()
                                                                      .toString(), new Point(shop.getX(), shop.getY())));
             }
             stringRedisTemplate.opsForGeo()
                                .add(key, locations);
         }


     }

     @Test
     void testHyperLogLog() {
         String[] values = new String[1000];
         int j = 0;
         for (int i = 0; i < 1000000; i++) {
             j = i % 1000;
             values[j] = "user_" + i;
             if (j == 999) {
                 stringRedisTemplate.opsForHyperLogLog()
                                    .add("hl", values);
             }
         }
         // 统计
         Long count = stringRedisTemplate.opsForHyperLogLog()
                                         .size("hl");
         System.out.println(count);
     }
 }
