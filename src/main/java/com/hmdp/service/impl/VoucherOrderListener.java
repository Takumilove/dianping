package com.hmdp.service.impl;

import com.alibaba.fastjson.JSON;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author RaoPengFei
 * @since 2024/7/19
 */
@Component
@RocketMQMessageListener(topic = "orderTopic", consumerGroup = "voucher-consumer-group", consumeMode =
        ConsumeMode.CONCURRENTLY)
@Slf4j
public class VoucherOrderListener implements RocketMQListener<MessageExt> {
    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public void onMessage(MessageExt messageExt) {
        try {
            String body = new String(messageExt.getBody());
            VoucherOrder voucherOrder = JSON.parseObject(body, VoucherOrder.class);

            log.info("接收到存储订单信息的消息", JSON.toJSON(voucherOrder)
                                                         .toString());

            // 更新库存
            boolean success = seckillVoucherService.update()
                                                   .setSql("stock=stock-1")
                                                   .eq("voucher_id", voucherOrder.getVoucherId())
                                                   .gt("stock", 0)
                                                   .update();

            // 保存订单
            voucherOrderService.save(voucherOrder);

            log.info("订单信息存储完成? {}", success);
        } catch (Exception e) {
            log.error("处理消息失败", e);
        }
    }
}
