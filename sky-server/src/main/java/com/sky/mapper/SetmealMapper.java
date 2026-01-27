package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @AutoFill(value = OperationType.INSERT)
    //@Options(useGeneratedKeys = true, keyProperty = "id")
        // 这个回显id不起作用是因为insert语句是配置在xml中的，注解先注入查找回显后才执行sql，数据尚未建立自然没有id
    void save(Setmeal setmeal);

    List<SetmealVO> list(SetmealPageQueryDTO setmealPageQueryDTO);

    void deleteBatch(List<Long> ids);

    Setmeal selectById(Long id);

    @AutoFill(value = OperationType.UPDATE)
    void update(Setmeal setmeal);

}
