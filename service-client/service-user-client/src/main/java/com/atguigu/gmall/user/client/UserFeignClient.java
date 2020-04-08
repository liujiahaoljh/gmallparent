package com.atguigu.gmall.user.client;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.impl.UserDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author jiahao
 * @create 2020-03-28 16:28
 */
@FeignClient(value = "service-user", fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    @GetMapping("/api/user/inner/findUserAddressListByUserId/{userId}")
    List<UserAddress> findUserAddressListByUserId(@PathVariable(value = "userId") String userId);

}
