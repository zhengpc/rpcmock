package com.alibaba.china.cntools.rpcmock.util;

import com.taobao.eagleeye.EagleEye;
import com.taobao.hsf.invocation.Invocation;
import com.taobao.hsf.model.ConsumerMethodModel;
import com.taobao.hsf.model.metadata.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.taobao.hsf.model.metadata.ServiceMetadata.WRITE_MODE_UNIT;

/**
 * @author zhengpc
 */
public class UnitKeyUtils {

    /**
     * 日志
     */
    private static final Logger rpcMockLog = LoggerFactory.getLogger("rpcMockLog");

    private static final String HSF_UNIT_UID = "_hsf_unit_uid";

    /**
     * @param invocation
     * @return
     */
    public static Long getCurrentUnitKey(Invocation invocation) {
        if (invocation == null) {
            return null;
        }

        try {
            ServiceMetadata serviceMetadata = null;
            if (invocation.isServerSide()) {
                serviceMetadata = Optional.ofNullable(invocation)
                        .map(Invocation::getServerInvocationContext)
                        .map(Invocation.ServerInvocationContext::getMetadata)
                        .orElse(null);
            } else {
                serviceMetadata = Optional.ofNullable(invocation)
                        .map(Invocation::getClientInvocationContext)
                        .map(Invocation.ClientInvocationContext::getMethodModel)
                        .map(ConsumerMethodModel::getMetadata)
                        .orElse(null);
            }

            Long currentUnitKey = null;
            if (serviceMetadata != null && WRITE_MODE_UNIT.equalsIgnoreCase(serviceMetadata.getWriteMode())) {
                int route = serviceMetadata.getRoute();
                Object[] methodArgs = invocation.getMethodArgs();
                if (methodArgs != null && route >= 0) {
                    currentUnitKey = (Long) methodArgs[route];
                }
                if (invocation.isServerSide() && currentUnitKey != null && EagleEye.getUserData(HSF_UNIT_UID) == null) {
                    EagleEye.putUserData(HSF_UNIT_UID, String.valueOf(currentUnitKey));
                }
            }

            if (currentUnitKey == null) {
                currentUnitKey = Optional.ofNullable(EagleEye.getUserData(HSF_UNIT_UID))
                        .map(Long::valueOf)
                        .orElse(null);
            }

            return currentUnitKey;
        } catch (Throwable e) {
            rpcMockLog.error(e.getMessage(), e);
        }

        return null;
    }

}
