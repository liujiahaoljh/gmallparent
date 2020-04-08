package com.atguigu.gmall.user.mapper;

/**
 * @author jiahao
 * @create 2020-03-25 15:34
 */
import com.atguigu.gmall.model.user.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}