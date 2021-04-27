package com.alibaba.china.cntools.rpcmock.filter.hsf;

import com.alibaba.china.cntools.model.DiamondKey;
import com.alibaba.china.cntools.rpcmock.config.MockObjectConfigCache;
import com.taobao.hsf.annotation.Order;
import com.taobao.hsf.domain.HSFResponse;
import com.taobao.hsf.invocation.Invocation;
import com.taobao.hsf.invocation.InvocationHandler;
import com.taobao.hsf.invocation.RPCResult;
import com.taobao.hsf.invocation.filter.RPCFilter;
import com.taobao.hsf.util.concurrent.Futures;
import com.taobao.hsf.util.concurrent.ListenableFuture;
import com.taobao.hsf.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.alibaba.china.cntools.rpcmock.util.UnitKeyUtils.getCurrentUnitKey;

/**
 * @author zhengpc
 */
@Order(Integer.MAX_VALUE)
public abstract class HSFResultMockFilter implements RPCFilter {

    /**
     * 日志
     */
    protected static final Logger rpcMockLog = LoggerFactory.getLogger("rpcMockLog");

    protected static final MockObjectConfigCache mockObjectConfigCache = new MockObjectConfigCache();

    @Override
    public ListenableFuture<RPCResult> invoke(InvocationHandler nextHandler, Invocation invocation) throws Throwable {
        try {
            if (enableHSFMockFunc()) {
                // 单元用户ID，如果是单元化接口，则此为买家ID，否则为空
                Long currentUnitKey = getCurrentUnitKey(invocation);

                // 获取调用签名
                String signature = getInovationSignature(invocation);
                // 根据调用签名判断是否需要调用结果的mock
                if (isGoToMock(currentUnitKey, signature)) {
                    // 获取mock的结果对象
                    Object mockObject = getMockObject(currentUnitKey, signature);
                    if (mockObject != null) {
                        SettableFuture<RPCResult> mockRPCFuture = Futures.createSettableFuture();
                        mockRPCFuture.set(createRPCResult(mockObject));
                        return mockRPCFuture;
                    }
                }
            }
        } catch (Throwable e) {
            rpcMockLog.error(e.getMessage(), e);
        }

        return nextHandler.invoke(invocation);
    }

    @Override
    public void onResponse(Invocation invocation, RPCResult rpcResult) {
    }

    /**
     * 是否开启HSF的Mock功能
     *
     * @return
     */
    protected abstract boolean enableHSFMockFunc();

    /**
     * 判断当前调用是否需要Mock调用结果
     *
     * @param currentUnitKey
     * @param signature
     * @return
     */
    protected abstract boolean isGoToMock(Long currentUnitKey, String signature);

    /**
     * 获取Mock的配置内容
     *
     * @param currentUnitKey
     * @param signature
     * @return
     */
    protected abstract List<DiamondKey> getMockConfigs(Long currentUnitKey, String signature);

    /**
     * 获取调用签名
     *
     * @param invocation
     * @return
     */
    protected String getInovationSignature(Invocation invocation) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(invocation.getTargetServiceUniqueName());
        stringBuilder.append(":");
        stringBuilder.append(invocation.getMethodName());
        stringBuilder.append('-');

        Optional.ofNullable(invocation.getMethodArgSigs())
                .map(Arrays::stream)
                .ifPresent(stream -> {
                    stream.forEach(argSig -> {
                        stringBuilder.append(argSig.charAt(argSig.lastIndexOf('.') + 1));
                    });
                });

        return stringBuilder.toString();
    }

    /**
     * 获取Mock对象
     *
     * @param currentUnitKey
     * @param signature
     * @return
     */
    protected Object getMockObject(Long currentUnitKey, String signature) {
        //
        List<DiamondKey> diamondKeys = getMockConfigs(currentUnitKey, signature);
        return Optional.ofNullable(diamondKeys)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .map(diamondKey -> mockObjectConfigCache.getValue(diamondKey))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * @param mockObject
     * @return
     */
    protected RPCResult createRPCResult(Object mockObject) {
        RPCResult rpcResult = new RPCResult();
        rpcResult.setHsfResponse(new HSFResponse());
        rpcResult.setAppResponse(mockObject);
        return rpcResult;
    }

}