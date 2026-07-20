package com.kyx.service.business.dal.mysql.oauth2;

import com.kyx.foundation.mybatis.core.mapper.BaseMapperX;
import com.kyx.service.business.dal.dataobject.oauth2.OAuth2CodeDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OAuth2CodeMapper extends BaseMapperX<OAuth2CodeDO> {

    default OAuth2CodeDO selectByCode(String code) {
        return selectOne(OAuth2CodeDO::getCode, code);
    }

}
