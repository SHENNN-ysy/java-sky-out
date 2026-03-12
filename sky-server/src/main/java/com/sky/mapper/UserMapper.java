package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    User getByOpenid(String openid);

    void save(User user);

    @Select(" select * from user where id=#{userId}")
    User getById(Long userId);
}
