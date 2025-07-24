package com.songhan.job;

import com.songhan.service.IOrderService;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 检测未接收到或未正确处理的支付回调通知
 * @create 2024-09-30 09:59
 */
@Slf4j
@Component()
/**
 * 定时任务：检测那些没有接收到支付宝支付回调（或未正确处理）的订单，
 * 并主动调用支付宝交易查询接口确认支付状态，如果已支付则手动更新订单状态。
 */
public class NoPayNotifyOrderJob {

    @Resource
    private IOrderService orderService;  // 注入订单服务，用于查询订单和更新订单状态

    @Resource
    private AlipayClient alipayClient;   // 注入支付宝客户端，用于调用支付宝开放平台接口

    /**
     * 每 3 秒执行一次任务，检测未收到支付回调的订单
     * cron 表达式："0/3 * * * * ?" 表示每隔 3 秒执行一次
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void exec() {
        try {
            log.info("任务：检测未接收到或未正确处理的支付回调通知");

            // 查询数据库中所有“未接收到支付通知”的订单号
            List<String> orderIds = orderService.queryNoPayNotifyOrder();

            // 如果没有此类订单，直接返回
            if (null == orderIds || orderIds.isEmpty()) return;

            // 遍历每一个订单，逐个查询支付宝的真实支付状态
            for (String orderId : orderIds) {
                // 构建支付宝交易查询请求对象
                AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

                // 设置请求参数：使用商户订单号查询
                AlipayTradeQueryModel bizModel = new AlipayTradeQueryModel();
                bizModel.setOutTradeNo(orderId); // 设置商户订单号
                request.setBizModel(bizModel);   // 绑定请求模型

                // 调用支付宝接口，执行查询操作
                AlipayTradeQueryResponse alipayTradeQueryResponse = alipayClient.execute(request);

                // 获取支付宝返回的响应状态码
                String code = alipayTradeQueryResponse.getCode();

                // 支付宝查询成功的状态码是 "10000"
                if ("10000".equals(code)) {
                    // 表示该订单已经支付成功，但之前没收到回调，所以手动修改订单状态
                    orderService.changeOrderPaySuccess(orderId);
                }
            }
        } catch (Exception e) {
            // 捕获异常并记录日志，防止任务线程挂掉
            log.error("检测未接收到或未正确处理的支付回调通知失败", e);
        }
    }
}
