package com.atguigu.gmall.user.client.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-28 16:31
 */
@Component
public class UserDegradeFeignClient implements UserFeignClient {


    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        return null;
    }
}
