package io.github.honhimw.beta;

import io.github.honhimw.IpUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * @author hon_him
 * @since 2023-05-11
 */

public class Log4j2IpPidSocketAppender extends SocketAppender {

    protected Log4j2IpPidSocketAppender(String name, Layout<? extends Serializable> layout, Filter filter, AbstractSocketManager manager, boolean ignoreExceptions, boolean immediateFlush, Advertiser advertiser, Property[] properties) {
        super(StringUtils.getIfBlank(name, () -> IpUtils.localIPv4() + "#" + getProcessID())
            , layout, filter, manager, ignoreExceptions, immediateFlush, advertiser, properties);
    }

    public static int getProcessID() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.parseInt(runtimeMXBean.getName().split("@")[0]);
    }

}
