package com.songhan.service.weixin;


import com.songhan.domain.vo.WeixinTemplateMessageVO;
import com.songhan.domain.req.WeixinQrCodeReq;
import com.songhan.domain.res.WeixinQrCodeRes;
import com.songhan.domain.res.WeixinTokenRes;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/*
* @description 微信API服务 retrofit2
* */
public interface IWeixinApiService {

    /**
     *
     * @param grantType 获取acce_token填写client_credential
     * @param appId 第三方用户唯一凭证
     * @param appSecret
     * @return
     */
    @GET("cgi-bin/token")
    Call<WeixinTokenRes> getToken(@Query("grant_type") String grantType,
                                        @Query("appid") String appId,
                                        @Query("secret") String appSecret);


    @POST("cgi-bin/qrcode/create")
    Call<WeixinQrCodeRes> createQrCode(@Query("access_token") String accessToken, @Body WeixinQrCodeReq weixinQrCodeReq);

    @POST("cgi-bin/message/template/send")
    Call<Void> sendMessage(@Query("access_token") String accessToken, @Body WeixinTemplateMessageVO weixinTemplateMessageVO);
}
