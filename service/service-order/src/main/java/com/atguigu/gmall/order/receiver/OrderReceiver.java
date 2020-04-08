package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-31 21:27
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    /**
     * 取消订单消费者
     * 延迟队列，不能再这里做交换机与队列绑定
     * @param orderId
     * @throws IOException
     */
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) throws IOException {
        if (null != orderId) {
            //防止重复消费
            OrderInfo orderInfo = orderService.getById(orderId);
            //判断 paymentInfo中有没有交易记录 支付宝中是否有交易记录
            /**
             *  面试可能会问你，关单的业务逻辑！
             *  判断 paymentInfo 中有没有交易记录，是否是未付款！
             *  判断 orderInfo 订单的状态
             *  判断在支付宝中是否有交易记录。
             *  如果有交易记录{扫描了二维码} alipayService.checkPayment(orderId)
             *  如果在支付宝中有交易记录，调用关闭支付的订单接口。如果正常关闭了，那么说明，用户根本没有付款。如果关闭失败。
             *      说明用户已经付款成功了。 发送消息队列更新订单的状态！ 通知仓库，减库存。
             *      rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
             *  关闭订单：
             *      1.  用户没有点到付款，二维码都没有出现。
             *      2.  系统出现了二维码，但是用户并没有扫描。
             *      3.  系统出现了二维码，用户扫了，但是没有输入密码。
             */

            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                //修改订单的状态 订单的状态变为closed
                orderService.execExpiredOrder(orderId);
            }
        }
        //手动ack
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    //监听支付完成之后的消息队列
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paySuccess(Long orderId, Message message, Channel channel) throws IOException {
        if (null != orderId){
            //防止重复消费
            OrderInfo orderInfo = orderService.getById(orderId);
            if(null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {
                // 支付成功！ 修改订单状态为已支付
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

            }
        }
        //收到确认消息处理完毕
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson, Message message, Channel channel) throws IOException {
        if (!StringUtils.isEmpty(msgJson)){
            //将json转化成为map集合
            Map<String,Object> map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String)map.get("orderId");
            String status = (String)map.get("status");
            if ("DEDUCTED".equals(status)){
                // 减库存成功！ 修改订单状态为已支付
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);
            }else {
        /*
            减库存失败！远程调用其他仓库查看是否有库存！
            true:   orderService.sendOrderStatus(orderId); orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
            false:  1.  补货  | 2.   人工客服。
         */
                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);
            }
        }
        //手动ack
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
