package com.alibaba.china.cntools.rpcmock.config;

import com.alibaba.china.cntools.cache.SimpleLocalCacheDiamondSupport;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

/**
 * @author zhengpc
 */
public class MockObjectConfigCache extends SimpleLocalCacheDiamondSupport<Object> {

    @Override
    protected Object dataConvert(String configValue) throws Exception {
        if (StringUtils.isNotBlank(configValue)) {
            JSONObject jsonObject = JSON.parseObject(configValue);
            String className = jsonObject == null ? null : jsonObject.getString("class");
            if (StringUtils.isNotBlank(className)) {
                return JSON.parseObject(configValue, Class.forName(className));
            }
        }
        return null;
    }

}
