package com.kyx.service.op.dal.dataobject.demo.demo03;

import com.kyx.foundation.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 学生课程 DO
 *
 * @author MK
 */
@TableName("foundation_demo03_course")
@KeySequence("foundation_demo03_course_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Demo03CourseDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 学生编号
     */
    private Long studentId;
    /**
     * 名字
     */
    private String name;
    /**
     * 分数
     */
    private Integer score;

}