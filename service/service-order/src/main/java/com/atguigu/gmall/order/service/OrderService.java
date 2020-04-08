package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author jiahao
 * @create 2020-03-28 21:52
 */
public interface OrderService extends IService<OrderInfo> {

    /**
     * 保存订单
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生产流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param userId 获取缓存中的流水号的key
     * @param tradeCodeNo   页面传递过来的流水号
     * @return
     */
    boolean checkTradeCode(String userId, String tradeCodeNo);


    /**
     * 删除流水号
     * @param userId 获取缓存中的流水号的key
     */
    void deleteTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 更新过期时间
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 更新商品的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单id查询订单对象（订单明细）
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 发送消息给库存！
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo转化成为json
     * @param orderInfo
     * @return
     */
    public Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单接口
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(long orderId, String wareSkuMap);
}
