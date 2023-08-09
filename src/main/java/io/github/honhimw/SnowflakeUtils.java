package io.github.honhimw;

import java.io.Serializable;

/**
 * Twitter雪花算法, 调整了{@link #workerIdBits}和{@link #dataCenterIdBits}大小
 * <a href="https://github.com/dromara/hutool/blob/56a2819861/hutool-core/src/main/java/cn/hutool/core/lang/Snowflake.java"/>
 *
 * @author hon_him
 * @since 2022-07-18
 */
@SuppressWarnings("unused")
public class SnowflakeUtils implements Serializable {

    private final long twepoch;
    private final long workerIdBits = 8L;
    // 最大支持机器节点数0~31，一共32个
    @SuppressWarnings({"PointlessBitwiseExpression", "FieldCanBeLocal"})
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long dataCenterIdBits = 2L;
    // 最大支持数据中心节点数0~31，一共32个
    @SuppressWarnings({"PointlessBitwiseExpression", "FieldCanBeLocal"})
    private final long maxDataCenterId = -1L ^ (-1L << dataCenterIdBits);
    // 序列号12位
    private final long sequenceBits = 12L;
    // 机器节点左移12位
    private final long workerIdShift = sequenceBits;
    // 数据中心节点左移17位
    private final long dataCenterIdShift = sequenceBits + workerIdBits;
    // 时间毫秒数左移22位
    private final long timestampLeftShift = sequenceBits + workerIdBits + dataCenterIdBits;
    // 序列掩码，用于限定序列最大值不能超过4095
    @SuppressWarnings("FieldCanBeLocal")
    private final long sequenceMask = ~(-1L << sequenceBits);// 4095

    private final long workerId;
    private final long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public static SnowflakeUtils getInstance() {
        return getInstance(1658914580040L);
    }
    public static SnowflakeUtils getInstance(long twepoch) {
        String ipv4 = IpUtils.localIPv4();
        String id3 = ipv4.split("\\.")[3];
        return getInstance(twepoch, Long.parseLong(id3), 0L);
    }

    public static SnowflakeUtils getInstance(long twepoch, long workerId, long dataCenterId) {
        return new SnowflakeUtils(twepoch, workerId, dataCenterId);
    }

    /**
     * @param twepoch        同种业务保持一致
     * @param workerId       工作机器节点id
     * @param dataCenterId   数据中心id
     */
    private SnowflakeUtils(long twepoch, long workerId, long dataCenterId) {
        if (twepoch < 0) {
            throw new IllegalArgumentException("twepoch can't be greater than %s or less than 0");
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                String.format("worker Id can't be greater than %s or less than 0", maxWorkerId));
        }
        if (dataCenterId > maxDataCenterId || dataCenterId < 0) {
            throw new IllegalArgumentException(
                String.format("datacenter Id can't be greater than %s or less than 0", maxDataCenterId));
        }
        this.twepoch = twepoch;
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    /**
     * 根据Snowflake的ID，获取机器id
     *
     * @param id snowflake算法生成的id
     * @return 所属机器的id
     */
    public long getWorkerId(long id) {
        return id >> workerIdShift & ~(-1L << workerIdBits);
    }

    /**
     * 根据Snowflake的ID，获取数据中心id
     *
     * @param id snowflake算法生成的id
     * @return 所属数据中心
     */
    public long getDataCenterId(long id) {
        return id >> dataCenterIdShift & ~(-1L << dataCenterIdBits);
    }

    /**
     * 根据Snowflake的ID，获取生成时间
     *
     * @param id snowflake算法生成的id
     * @return 生成的时间
     */
    public long getGenerateDateTime(long id) {
        return (id >> timestampLeftShift & ~(-1L << 41L)) + twepoch;
    }

    /**
     * 下一个ID
     *
     * @return ID
     */
    public synchronized long nextId() {
        long timestamp = timestamp();
        if (timestamp < lastTimestamp) {
            if (lastTimestamp - timestamp < 2000) {
                // 容忍2秒内的回拨，避免NTP校时造成的异常
                timestamp = lastTimestamp;
            } else {
                // 如果服务器时间有问题(时钟后退) 报错。
                throw new IllegalStateException(String.format("Clock moved backwards. Refusing to generate id for %sms",
                    lastTimestamp - timestamp));
            }
        }

        if (timestamp == lastTimestamp) {
            final long seq = (sequence + 1) & sequenceMask;
            if (seq == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
            sequence = seq;
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift) | (dataCenterId
            << dataCenterIdShift) | (workerId << workerIdShift) | sequence;
    }

    /**
     * 下一个ID（字符串形式）
     *
     * @return ID 字符串形式
     */
    public String nextIdStr() {
        return Long.toString(nextId());
    }

    // ------------------------------------------------------------------------------------------------------------------------------------ Private method start

    /**
     * 循环等待下一个时间
     *
     * @param lastTimestamp 上次记录的时间
     * @return 下一个时间
     */
    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = timestamp();
        // 循环直到操作系统时间戳变化
        while (timestamp == lastTimestamp) {
            timestamp = timestamp();
        }
        if (timestamp < lastTimestamp) {
            // 如果发现新的时间戳比上次记录的时间戳数值小，说明操作系统时间发生了倒退，报错
            throw new IllegalStateException(
                String.format("Clock moved backwards. Refusing to generate id for %sms", lastTimestamp - timestamp));
        }
        return timestamp;
    }

    /**
     * 生成时间戳
     *
     * @return 时间戳
     */
    private static long timestamp() {
        return System.currentTimeMillis();
    }



}
