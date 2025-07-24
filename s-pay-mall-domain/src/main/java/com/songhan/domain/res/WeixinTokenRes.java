package com.songhan.domain.res;


import lombok.Data;

/*
*
* @description 获取Access token DTO对象
* */

/**
 *
 */
@Data
public class WeixinTokenRes {


    private String access_token;
    private String expires_in;
    private String errcode;
    private String errmsg;
}
