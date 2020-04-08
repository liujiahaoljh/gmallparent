package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.QuerydslRepositoryInvokerAdapter;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * @author jiahao
 * @date 2020/4/1 10:51
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    // 实现类 ，通常调用mapper
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private AlipayService alipayService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        // 得到数据，应该是从orderInfo 中获取。
        // 当前同一个订单，能否进行多次交易？ 不能！
        // select count(*) as count from payment_info where order_id = orderInfo.getId() and payment_type = paymentType
        // count > 0
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderInfo.getId()).eq("payment_type",paymentType);

        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (count>0) return;

        // 给paymentInfo 赋值
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        // 一开始生成二维码的时候是未付款，当用户支付完成 之后，得到支付宝的异步回调之后。那么我们要更新支付的状态 PAID
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        // 总金额
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());

        // 保存交易记录
        paymentInfoMapper.insert(paymentInfo);

    }

    @Override
    public PaymentInfo getPaymentInfo(String out_trade_no, String name) {
        // select * from payment_info where out_trade_no = out_trade_no and payment_type=ALIPAY
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",out_trade_no).eq("payment_type",name);
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    @Override
    public void paySuccess(String out_trade_no, String name, Map<String, String> paramMap) {
        //需要通过查询来获取paymentInfo中的orderId
        PaymentInfo paymentInfo = getPaymentInfo(out_trade_no, name);
        //判断当前的paymentInfo的支付状态
        if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name())||
         paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED)){
            return;
        }


        // update payment_info set payment_status=PAID ,callback_time= new Date() ,callback_content=paramMap where out_trade_no = out_trade_no and payment_type=ALIPAY
        PaymentInfo paymentInfoUPd = new PaymentInfo();
        paymentInfoUPd.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfoUPd.setCallbackTime(new Date());
        paymentInfoUPd.setCallbackContent(paramMap.toString());
        // 获取支付宝的交易编号！
        paymentInfoUPd.setTradeNo(paramMap.get("trade_no"));
        // 调用自己的更新方法
        updatePymentInfo(out_trade_no,paymentInfoUPd);

        //发送一个消息通知订单，修改订单状态
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());


    }

    /**
     * 更新交易记录
     * @param out_trade_no
     * @param paymentInfoUPd
     */
    public void updatePymentInfo(String out_trade_no, PaymentInfo paymentInfoUPd) {
        // 构造更新的条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",out_trade_no);
        paymentInfoMapper.update(paymentInfoUPd,paymentInfoQueryWrapper);
    }

    @Override
    public void closePayment(Long orderId) {
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id",orderId);
        //如果当前消息记录不存在，不需要更新消息记录
        Integer count = paymentInfoMapper.selectCount(wrapper);
        if (count == null || count.intValue()==0)return;
        //在关闭支付宝交易之前，还需要关闭paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        paymentInfoMapper.update(paymentInfo,wrapper);
        //关闭支付宝的交易
        alipayService.closePay(orderId);

    }
}
