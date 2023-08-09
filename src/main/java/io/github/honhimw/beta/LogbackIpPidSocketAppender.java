package io.github.honhimw.beta;

import ch.qos.logback.classic.net.SocketAppender;
import io.github.honhimw.IpUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author hon_him
 * @since 2022-09-20
 */
@SuppressWarnings("unused")
public class LogbackIpPidSocketAppender extends SocketAppender {

    private static final AtomicBoolean SHIFT = new AtomicBoolean(false);

    @Override
    public void start() {
        synchronized (SHIFT) {
            if (!SHIFT.get()) {
                try {
                    this.getContext().setName(IpUtils.localIPv4() + "#" + getProcessID());
                } catch (Exception ignored) {
                }
                SHIFT.compareAndSet(false, true);
            }
        }
        super.start();
    }

    public static int getProcessID() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return Integer.parseInt(runtimeMXBean.getName().split("@")[0]);
    }

}
