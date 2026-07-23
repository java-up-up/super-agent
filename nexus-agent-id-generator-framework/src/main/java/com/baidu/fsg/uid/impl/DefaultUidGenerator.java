package com.baidu.fsg.uid.impl;

import com.baidu.fsg.uid.BitsAllocator;
import com.baidu.fsg.uid.UidGenerator;
import com.baidu.fsg.uid.exception.UidGenerateException;
import com.baidu.fsg.uid.utils.AbstractDateUtils;
import com.baidu.fsg.uid.worker.WorkerIdAssigner;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 生成器
 * @author: 阿星不是程序员
 **/

public class DefaultUidGenerator implements UidGenerator, InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultUidGenerator.class);

    protected int timeBits = 28;
    protected int workerBits = 22;
    protected int seqBits = 13;

    protected String epochStr = "2024-05-20";
    protected long epochSeconds = TimeUnit.MILLISECONDS.toSeconds(1716134400000L);

    protected BitsAllocator bitsAllocator;
    protected long workerId;

    protected long sequence = 0L;
    protected long lastSecond = -1L;

    protected WorkerIdAssigner workerIdAssigner;

    protected SnowflakeIdGenerator snowflakeIdGenerator;

    @Override
    public void afterPropertiesSet() throws Exception {

        bitsAllocator = new BitsAllocator(timeBits, workerBits, seqBits);

        workerId = workerIdAssigner.assignWorkerId();
        if (workerId > bitsAllocator.getMaxWorkerId()) {
            throw new RuntimeException("Worker id " + workerId + " exceeds the max " + bitsAllocator.getMaxWorkerId());
        }

        LOGGER.info("Initialized bits(1, {}, {}, {}) for workerID:{}", timeBits, workerBits, seqBits, workerId);
    }

    @Override
    public long getUid() throws UidGenerateException {
        try {
            return nextId();
        } catch (Exception e) {
            LOGGER.error("Generate unique id exception. ", e);
            throw new UidGenerateException(e);
        }
    }

    @Override
    public long getId(){
        return snowflakeIdGenerator.nextId();
    }

    @Deprecated
    @Override
    public long getOrderNumber(long userId,long tableCount,long databaseCount) {
        return snowflakeIdGenerator.getOrderNumber(userId,tableCount,databaseCount);
    }

    @Override
    public long getOrderNumber(long userId) {
        return snowflakeIdGenerator.getOrderNumber(userId);
    }

    @Deprecated
    @Override
    public long getOrderNumber(long userId, long tableCount) {
        return snowflakeIdGenerator.getOrderNumber(userId,tableCount);
    }

    @Override
    public String parseUid(long uid) {
        long totalBits = BitsAllocator.TOTAL_BITS;
        long signBits = bitsAllocator.getSignBits();
        long timestampBits = bitsAllocator.getTimestampBits();
        long workerIdBits = bitsAllocator.getWorkerIdBits();
        long sequenceBits = bitsAllocator.getSequenceBits();

        long sequence = (uid << (totalBits - sequenceBits)) >>> (totalBits - sequenceBits);
        long workerId = (uid << (timestampBits + signBits)) >>> (totalBits - workerIdBits);
        long deltaSeconds = uid >>> (workerIdBits + sequenceBits);

        Date thatTime = new Date(TimeUnit.SECONDS.toMillis(epochSeconds + deltaSeconds));
        String thatTimeStr = AbstractDateUtils.formatByDateTimePattern(thatTime);

        return String.format("{\"UID\":\"%d\",\"timestamp\":\"%s\",\"workerId\":\"%d\",\"sequence\":\"%d\"}",
                uid, thatTimeStr, workerId, sequence);
    }

    protected synchronized long nextId() {
        long currentSecond = getCurrentSecond();

        if (currentSecond < lastSecond) {
            long refusedSeconds = lastSecond - currentSecond;
            throw new UidGenerateException("Clock moved backwards. Refusing for %d seconds", refusedSeconds);
        }

        if (currentSecond == lastSecond) {
            sequence = (sequence + 1) & bitsAllocator.getMaxSequence();

            if (sequence == 0) {
                currentSecond = getNextSecond(lastSecond);
            }

        } else {
            sequence = 0L;
        }

        lastSecond = currentSecond;

        return bitsAllocator.allocate(currentSecond - epochSeconds, workerId, sequence);
    }

    private long getNextSecond(long lastTimestamp) {
        long timestamp = getCurrentSecond();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentSecond();
        }

        return timestamp;
    }

    private long getCurrentSecond() {
        long currentSecond = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        if (currentSecond - epochSeconds > bitsAllocator.getMaxDeltaSeconds()) {
            throw new UidGenerateException("Timestamp bits is exhausted. Refusing UID generate. Now: " + currentSecond);
        }

        return currentSecond;
    }

    public void setWorkerIdAssigner(WorkerIdAssigner workerIdAssigner) {
        this.workerIdAssigner = workerIdAssigner;
    }

    public void setTimeBits(int timeBits) {
        if (timeBits > 0) {
            this.timeBits = timeBits;
        }
    }

    public void setWorkerBits(int workerBits) {
        if (workerBits > 0) {
            this.workerBits = workerBits;
        }
    }

    public void setSeqBits(int seqBits) {
        if (seqBits > 0) {
            this.seqBits = seqBits;
        }
    }

    public void setEpochStr(String epochStr) {
        if (StringUtils.isNotBlank(epochStr)) {
            this.epochStr = epochStr;
            this.epochSeconds = TimeUnit.MILLISECONDS.toSeconds(AbstractDateUtils.parseByDayPattern(epochStr).getTime());
        }
    }

    public void setSnowflakeIdGenerator(final SnowflakeIdGenerator snowflakeIdGenerator) {
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }
}
