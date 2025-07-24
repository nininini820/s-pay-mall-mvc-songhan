package com.songhan.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.google.common.eventbus.EventBus;
import com.songhan.common.constants.Constants;
import com.songhan.dao.IOrderDao;
import com.songhan.domain.po.PayOrder;
import com.songhan.domain.req.ShopCartReq;
import com.songhan.domain.res.PayOrderRes;
import com.songhan.domain.vo.ProductVO;
import com.songhan.service.IOrderService;
import com.songhan.service.rpc.ProductRPC;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements IOrderService {


    @Value("${alipay.notify_url}")
    private String notifyUrl;
    @Value("${alipay.return_url}")
    private String returnUrl;

    @Resource
    private IOrderDao orderDao;

    @Resource
    private ProductRPC productRPC;

    @Resource
    private AlipayClient alipayClient;

    @Resource
    private EventBus eventBus;





    @Override
    public PayOrderRes createOrder(ShopCartReq shopCartReq) throws Exception {


        // 1. 查询当前用户是否存在未支付订单或掉单订单
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setUserId(shopCartReq.getUserId());
        payOrderReq.setProductId(shopCartReq.getProductId());

        PayOrder unpaidOrder = orderDao.queryUnPayOrder(payOrderReq);

        if (unpaidOrder != null && Constants.OrderStatusEnum.PAY_WAIT.getCode().equals(unpaidOrder.getStatus())) {
            log.info("创建订单-存在，因存在未支付订单，userId:{} productId:{} orderId:{}", unpaidOrder.getUserId(), unpaidOrder.getProductId(), unpaidOrder.getOrderId());
            return PayOrderRes.builder()
                    .orderId(unpaidOrder.getOrderId())
//                    .userId(unpaidOrder.getUserId())
                    .payUrl(unpaidOrder.getPayUrl())
                    .build();

        } else if (unpaidOrder != null && Constants.OrderStatusEnum.CREATE.getCode().equals(unpaidOrder.getStatus())) {
            //todo

            PayOrder payOrder = doPrepayOrder(unpaidOrder.getProductId(), unpaidOrder.getProductId(),unpaidOrder.getOrderId(), unpaidOrder.getTotalAmount());

            return PayOrderRes.builder()
                    .orderId(payOrder.getOrderId())
                    .payUrl(payOrder.getPayUrl())
                    .build();
        }

        // 2. 查询商品 & 创建订单

        ProductVO productVO = productRPC.queryProductByProductId(shopCartReq.getProductId());
        String orderId = RandomStringUtils.randomNumeric(16);
        orderDao.insert(PayOrder.builder()
                .userId(shopCartReq.getUserId())
                .productId(shopCartReq.getProductId())
                .productName(productVO.getProductName())
                .orderId(orderId)
                .totalAmount(productVO.getPrice())
                .orderTime(new Date())
                .status(Constants.OrderStatusEnum.CREATE.getCode())
                .build());

        //3. 创建支付单 todo

        PayOrder payOrder = doPrepayOrder(productVO.getProductId(), productVO.getProductId(),orderId, productVO.getPrice());


        return PayOrderRes.builder()
                .orderId(orderId)
                .payUrl(payOrder.getPayUrl())
                .build();
    }

    @Override
    public void changeOrderPaySuccess(String orderId) {
        PayOrder payOrderReq = new PayOrder();
        payOrderReq.setOrderId(orderId);
        payOrderReq.setStatus(Constants.OrderStatusEnum.PAY_SUCCESS.getCode());

        orderDao.changeOrderPaySuccess(payOrderReq);
        eventBus.post(JSON.toJSONString(payOrderReq));

    }

    @Override
    public List<String> queryNoPayNotifyOrder() {
        return orderDao.queryNoPayNotifyOrder();
    }

    @Override
    public List<String> queryTimeoutCloseOrderList() {
        return orderDao.queryTimeoutCloseOrderList();
    }

    @Override
    public boolean changeOrderClose(String orderId) {
        return orderDao.changeOrderClose(orderId);
    }

    /**
     * 生成支付宝网页支付订单，并更新数据库中对应订单的支付信息。
     *
     * @param productId    商品ID
     * @param productName  商品名称
     * @param orderId      订单号
     * @param totalAmount  订单总金额
     * @return             生成后的支付订单对象（包含支付宝跳转URL）
     */
    private PayOrder doPrepayOrder(String productId, String productName, String orderId, BigDecimal totalAmount) throws AlipayApiException {
        // 创建支付宝的网页支付请求对象
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        // 设置异步通知地址（支付宝支付完成后回调的接口）
        request.setNotifyUrl(notifyUrl);

        // 设置同步返回地址（用户支付成功后跳转的页面）
        request.setReturnUrl(returnUrl);

        // 构造业务参数 JSON 对象（支付宝接口要求的字段）
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderId);               // 商户订单号
        bizContent.put("total_amount", totalAmount.toString()); // 订单金额，必须为字符串类型
        bizContent.put("subject", productName);                // 商品标题
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY"); // 支付产品码：网页支付固定为这个值

        // 将业务参数设置到请求中
        request.setBizContent(bizContent.toString());

        // 调用支付宝 SDK，执行请求，获取支付表单（HTML form）
        String form = alipayClient.pageExecute(request).getBody();

        // 构造 PayOrder 对象，设置订单号、支付链接、状态为“等待支付”
        PayOrder payOrder = new PayOrder();
        payOrder.setOrderId(orderId);                     // 设置订单号
        payOrder.setPayUrl(form);                         // 设置支付链接（返回给前端跳转）
        payOrder.setStatus(Constants.OrderStatusEnum.PAY_WAIT.getCode()); // 设置订单状态为“等待支付”

        // 更新数据库中该订单的支付信息（更新支付链接和状态）
        orderDao.updateOrderPayInfo(payOrder);

        // 返回带有支付信息的 PayOrder 对象
        return payOrder;
    }



}
