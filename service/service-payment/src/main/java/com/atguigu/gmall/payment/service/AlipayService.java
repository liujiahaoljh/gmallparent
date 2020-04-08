package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author jiahao
 * @create 2020-04-01 16:57
 * 支付宝支付的接口
 */
public interface AlipayService {
    // 支付是根据订单Id 支付
    // 返回String 数据类型是因为 要将二维码显示到浏览器上！ @ResponseBody
    String careteAliPay(Long orderId) throws AlipayApiException;

    // 退款接口
    boolean refund(Long orderId);

    /**
     * 关闭交易
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);
    /**
     * 根据订单查询是否支付成功！
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}

