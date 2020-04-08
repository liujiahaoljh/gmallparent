package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClient;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.plugin2.message.Message;

import java.util.*;

/**
 * @author jiahao
 * @create 2020-03-28 21:53
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareURL;

    @Transactional //多张表进行保存必须加事务
    @Override
    public Long saveOrderInfo(OrderInfo orderInfo) {
        // 保存数据orderInfo,orderDetail
        // orderInfo 中，没有总金额，订单状态，userId,第三方交易编号，创建时间，过期时间，订单的主题，进程状态。
        // 计算总金额赋值给OrderInfo
        // 不需要重新赋值订单明细集合，因为在页面传递过来的时候，会自动封装好了
        // 根据springmvc 对象传值的规则，自动赋值到orderInfo.orderDetailList
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        // 用户Id 通过控制器获取的！
        String outTradeNo =  "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo.setCreateTime(new Date());
        // 过期时间 1 天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 订单主题：可用给个固定的字符串 或者 获取订单明细中的名称
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer tradeBody = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
            tradeBody.append(orderDetail.getSkuName()+"");
        }
        if (tradeBody.toString().length()>100){
            orderInfo.setTradeBody(tradeBody.toString().substring(0,100));
        }
        orderInfo.setTradeBody(tradeBody.toString());
        // 设置进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        orderInfoMapper.insert(orderInfo);

        // orderDetail 没有orderId = orderInfo.id
        for (OrderDetail orderDetail : orderDetailList) {
            // 防止主键冲突! id 自动增长
            // orderInfo.getId() 之所以能获取到Id 是因为在前面先做了插入数据
            orderDetail.setOrderId(orderInfo.getId());
            // 插入数据
            orderDetailMapper.insert(orderDetail);
        }
        //发送消息
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(),
                MqConst.DELAY_TIME);

        // 返回我们的订单id
        return orderInfo.getId();
    }

    //生产流水号
    @Override
    public String getTradeNo(String userId) {
        //定义流水号
        String tradeNo = UUID.randomUUID().toString().replace("-","");
        //将tradeNo 放入缓存
        //定义缓存的key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        //存储流水号
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        //返回流水号
        return tradeNo;
    }

    //比较流水号
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        //定义缓存的key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        String tradeNoRedis = (String) redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNoRedis.equals(tradeCodeNo);
    }

    //删除流水号
    @Override
    public void deleteTradeNo(String userId) {
        // 定义key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // 删除数据
        redisTemplate.delete(tradeNoKey);

    }

    //验证库存
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        //调用库存系统的接口
        String result = HttpClientUtil.doGet(wareURL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);


        return "1".equals(result);
    }

    //更新过期时间
    @Override
    public void execExpiredOrder(Long orderId) {
        //调用方法
        updateOrderStatus(orderId, ProcessStatus.CLOSED);
        //大宋消息队列，关闭支付宝的交易记录
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE, orderId);

    }

    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        // 订单主表
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        // 查询订单明细
        QueryWrapper<OrderDetail> orderDetailQueryWrapper = new QueryWrapper<>();
        orderDetailQueryWrapper.eq("order_id",orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.selectList(orderDetailQueryWrapper);
        // 将订单明细集合放入订单中
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    //实现减库存
    @Override
    public void sendOrderStatus(Long orderId) {
        updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);//通知仓库
        //发送json字符串给库存系统
        String wareJson = initWareOrder(orderId);
        //发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK, wareJson);
    }

    //通过订单id，查询orderInfo 然后转化为字符串
    private String initWareOrder(Long orderId) {
        //通过orderId获取orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        //准备组成json字符串
        //将需要的字符安转化为json字符串
        Map map = initWareOrder(orderInfo);
        //返回json字符串
        return JSON.toJSONString(map);
    }

    //转化orderINfo中的字段为map
    public Map initWareOrder(OrderInfo orderInfo) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        //收货人
        map.put("consignee", orderInfo.getConsignee());
        //收货电话
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        //订单备注
        map.put("orderComment", orderInfo.getOrderComment());
        //订单概要
        map.put("orderBody", orderInfo.getTradeBody());
        //收货地址
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        //付款方式
        map.put("paymentWay", "2");
        map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        /*
        details:[{skuId:101,skuNum:1,skuName: ’小米手64G’},
                 { skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        ArrayList<Map> mapArrayList = new ArrayList<>();
        //获取原始数据
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("skuId", orderDetail.getSkuId());
            hashMap.put("skuNum", orderDetail.getSkuNum());
            hashMap.put("skuName", orderDetail.getSkuName());
            mapArrayList.add(hashMap);
        }
        map.put("details", mapArrayList);
        return map;

    }

    @Override
    public List<OrderInfo> orderSplit(long orderId, String wareSkuMap) {
        // 声明一个子订单集合
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
          /*
            1. 先获取原始订单，我要知道谁被拆。
            2. 需要分解 wareSkuMap[{"wareId":"1","skuIds":["17"]},{"wareId":"2","skuIds":["21"]}] 对象
               变成我们java 语言能操作的对象
            3. 创建子订单
            4. 给子订单赋值
            5. 保存子订单到数据
            6. 修改订单的状态！
         */
          //获取原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // JSON
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        if (mapList!=null && mapList.size()>0){
            // 循环判断
            for (Map map : mapList) {
                // 获取仓库Id
                String wareId = (String) map.get("wareId");
                // 获取仓库Id 锁对应的商品Id
                List<String> skuIdList = (List<String>) map.get("skuIds");
                // 子订单
                OrderInfo subOrderInfo = new OrderInfo();
                // 给子订单赋值
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                // 为了防止子订单主键冲突，将id 设置为null
                subOrderInfo.setId(null);
                // 赋值父Id
                subOrderInfo.setParentOrderId(orderId);
                // 设置一个仓库Id
                subOrderInfo.setWareId(wareId);
                // 设置子订单的明细
                // 先获取原始订单的明细
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                // 声明一个订单明细集合
                List<OrderDetail> orderDetailArrayList = new ArrayList<>();
                // 判断
                if (orderDetailList!=null && orderDetailList.size()>0){
                    for (OrderDetail orderDetail : orderDetailList) {
                        for (String skuId : skuIdList) {
                            if (Long.parseLong(skuId)==orderDetail.getSkuId().longValue()){
                                // 添加订单明细
                                orderDetailArrayList.add(orderDetail);
                            }
                        }
                    }
                }
                // 子订单集合放入子订单中
                subOrderInfo.setOrderDetailList(orderDetailArrayList);
                // 计算价格
                subOrderInfo.sumTotalAmount();
                // 子订单保存上
                saveOrderInfo(subOrderInfo);
                // 添加子订单到集合中
                subOrderInfoList.add(subOrderInfo);
            }
        }
        // 修改原始订单的状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);

        return subOrderInfoList;
    }

}
