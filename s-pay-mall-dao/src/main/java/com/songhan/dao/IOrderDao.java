package com.songhan.dao;

import com.songhan.domain.po.PayOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IOrderDao {

    void insert(PayOrder order);

    PayOrder queryUnPayOrder(PayOrder payOrderReq);

    void updateOrderPayInfo(PayOrder payOrder);

    void changeOrderPaySuccess(PayOrder order);

    List<String> queryNoPayNotifyOrder();

    List<String> queryTimeoutCloseOrderList();

    boolean changeOrderClose(String orderId);


}
