package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.EmployeeMapper;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {
    private  static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;
    @Override
    public User wxlogin(UserLoginDTO userLoginDTO){
        //  调用微信接口服务，获取openid
        String openid = getOpenId(userLoginDTO.getCode());
        //  判断openid是否存在，不存在则报错
        if (openid == null){
            throw  new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //  openid存在，判断是否为新用户

        User user = userMapper.getByOpenid(openid);
        //  新用户，自动完成注册
        if (user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.save(user);
        }
        //  返回用户信息
        return user;
    }
    private  String getOpenId(String code){
        Map<String, String> Params = new HashMap<>();
        Params.put("appid", weChatProperties.getAppid());
        Params.put("secret", weChatProperties.getSecret());
        Params.put("js_code", code);
        Params.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN_URL, Params);
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
