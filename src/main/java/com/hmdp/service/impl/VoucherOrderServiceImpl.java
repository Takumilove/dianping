package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    // @PostConstruct
    // private void init() {
    //     SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    // }

/*
    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";

        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取消息队列中的订单信息 xreadgroup group g1 c1 count 1 block 2000 streams streams.order >
                    List<MapRecord<String, Object, Object>> list =
                            stringRedisTemplate.opsForStream()
                                               .read(
                                                       Consumer.from("g1", "c1"),
                                                       StreamReadOptions.empty()
                                                                        .count(1)
                                                                        .block(Duration.ofSeconds(2)),
                                                       StreamOffset.create(queueName, ReadOffset.lastConsumed()));
                    // 2.判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        // 2.1.如果获取失败，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    // 3.如果获取成功，处理订单
                    handleVoucherOrder(voucherOrder);
                    // 4.XACK确认 sack stream.orders g1 id
                    stringRedisTemplate.opsForStream()
                                       .acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取pending-list队列中的订单信息 xreadgroup group g1 c1 count 1  streams streams.order 0
                    List<MapRecord<String, Object, Object>> list =
                            stringRedisTemplate.opsForStream()
                                               .read(
                                                       Consumer.from("g1", "c1"),
                                                       StreamReadOptions.empty()
                                                                        .count(1),
                                                       StreamOffset.create(queueName, ReadOffset.from("0")));
                    // 2.判断消息获取是否成功
                    if (list == null || list.isEmpty()) {
                        // 2.1.如果获取失败，说明pending-list没有消息，结束循环
                        break;
                    }
                    // 3.解析消息中的订单信息
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    // 3.如果获取成功，处理订单
                    handleVoucherOrder(voucherOrder);
                    // 4.ACK确认 sack stream.orders g1 id
                    stringRedisTemplate.opsForStream()
                                       .acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pending-list订单异常", e);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
*/

/*    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取锁成功
        if (!isLock) {
            // 获取锁失败
            log.error("不允许重复下单");
            return;
        }
        try {
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }*/

/*
    private IVoucherOrderService proxy;
*/

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser()
                                .getId();
        // 生成订单id
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(),
                userId.toString(), String.valueOf(orderId));
        // 2.判断结果是为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1.不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 获取代理对象（事务）
        // proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 放mq，异步操作
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);

        // 发送消息到RocketMQ
        rocketMQTemplate.asyncSend("orderTopic", voucherOrder, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("发送成功");
            }

            @Override
            public void onException(Throwable throwable) {
                log.info("发送失败:" + throwable.getMessage());
                log.info("订单是：" + voucherOrder);
            }
        });
        // 3.返回订单id
        return Result.ok(orderId);
    }

/*    @Override
    public Result seckillVoucher(Long voucherId) {
        // 获取用户
        Long userId = UserHolder.getUser().getId();
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        // 2.判断结果是为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1.不为0，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        // 2.2为0，代表有购买资格，把下单信息保存到阻塞队列中‘
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3.设置订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.4.设置用户id
        voucherOrder.setUserId(userId);
        // 2.5.设置优惠券id
        voucherOrder.setVoucherId(voucherId);
        // 2.6.放入阻塞队列
        orderTasks.add(voucherOrder);
        // 获取代理对象（事务）
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        // 3.返回订单id
        return Result.ok(orderId);
    }*/

    /*@Override
    public Result seckillVoucher(Long voucherId) {
        // 1.查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        // 2.判断
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        // 3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }
        Long userId = UserHolder.getUser().getId();
        // 创建锁对象
        // SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取锁成功
        if (!isLock) {
            // 获取锁失败
            return Result.fail("不允许重复下单！");
        }
        try {
            // 获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.crateVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }*/

   /* @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 5.一人一单
        Long userId = voucherOrder.getUserId();

        // 5.1查询订单
        int count = query().eq("user_id", userId)
                           .eq("voucher_id", voucherOrder.getVoucherId())
                           .count();
        // 5.2判断是否存在
        if (count > 0) {
            // 已经购买过
            log.error("不允许重复下单！");
            return;
        }
        // 6.扣减库存
        boolean success = seckillVoucherService.update()
                                               .setSql("stock = stock - 1")
                                               .eq("voucher_id",
                                                       voucherOrder.getVoucherId())
                                               // where id=? and stock>0 防止超卖
                                               .gt("stock", 0)
                                               .update();
        if (!success) {
            // 扣减库存失败
            log.error("库存不足！");
            return;
        }
        // 7.创建订单
        save(voucherOrder);
    }*/

/*    @Override
    @Transactional
    public void crateVoucherOrder(VoucherOrder voucherOrder) {
        // 5.一人一单
        Long userId = UserHolder.getUser().getId();

        // 创建锁对象
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        // 尝试获取锁
        boolean isLock = redisLock.tryLock();

        if (!isLock) {
            // 获取锁失败，直接返回失败或者重试
            return Result.fail("不允许重复下单！");
        }

        try {
            // 5.1查询订单
            int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder).count();
            // 5.2判断是否存在
            if (count > 0) {
                // 已经购买过
                return Result.fail("用户已经购买过一次！");
            }
            // 6.扣减库存
            boolean success = seckillVoucherService.update().setSql("stock = stock - 1").eq("voucher_id", voucherOrder)
                    // where id=? and stock>0 防止超卖
                    .gt("stock", 0).update();
            if (!success) {
                // 扣减库存失败
                return Result.fail("库存不足！");
            }
            // 7.创建订单
            save(voucherOrder);
        } finally {
            // 释放锁
            redisLock.unlock();
        }
        return null;
    }*/
}