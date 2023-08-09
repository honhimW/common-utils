package io.github.honhimw.beta;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author hon_him
 * @since 2023-02-22
 */
public class Timer {
    
    private static final Logger log = LoggerFactory.getLogger(Timer.class);

    private long start;

    @Setter
    private Logger logger;

    @Setter
    private Function<Long, String> formatter = aLong -> Duration.ofMillis(aLong).toString();

    @Setter
    private Level level = Level.DEBUG;

    private Timer() {
        reset();
    }

    private Logger logger() {
        return Objects.requireNonNullElse(logger, log);
    }

    public static Timer getInstance() {
        return new Timer();
    }

    public static Timer getInstance(Logger logger) {
        Timer timer = new Timer();
        timer.setLogger(logger);
        return timer;
    }

    public void reset() {
        start = System.currentTimeMillis();
    }

    public long get() {
        return System.currentTimeMillis() - start;
    }

    public String str() {
        return Duration.ofMillis(get()).toString();
    }

    public void sout() {
        final long duration = get();
        System.out.println(this.formatter.apply(duration));
    }

    public void sout(String msg) {
        final long duration = get();
        System.out.println(String.format("%s: %s", msg, this.formatter.apply(duration)));
    }

    public void log() {
        final long duration = get();
        _log(formatter.apply(duration));
    }
    public void log(String msg) {
        final long duration = get();
        _log(String.format("%s: %s", msg, formatter.apply(duration)));

    }

    private void _log(String msg) {
        switch (level) {
            case INFO -> {
                if (logger().isInfoEnabled()) {
                    logger().info(msg);
                }
            }
            case DEBUG -> {
                if (logger().isDebugEnabled()) {
                    logger().debug(msg);
                }
            }
            case ERROR -> {
                if (logger().isErrorEnabled()) {
                    logger().error(msg);
                }
            }
            case WARN -> {
                if (logger().isWarnEnabled()) {
                    logger().warn(msg);
                }
            }
            case TRACE -> {
                if (logger().isTraceEnabled()) {
                    logger().trace(msg);
                }
            }
        }
    }

}
