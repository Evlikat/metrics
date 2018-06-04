package net.evlikat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.PriorityQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public class StatService {

    private static final Logger LOG = LoggerFactory.getLogger(StatService.class);

    private static final int MINUTE = 60 * 1000;

    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    private final Supplier<Long> now;
    private final PriorityQueue<Transaction> transactions =
            new PriorityQueue<>(
                    MINUTE, Comparator.comparing(Transaction::timestamp));
    private volatile DoubleSummaryStatistics lastMinuteStatistics = new DoubleSummaryStatistics();
    private final ScheduledExecutorService executorService;

    public StatService(Supplier<Long> currentTimestampProvider, ScheduledExecutorService executorService) {
        this.now = currentTimestampProvider;
        this.executorService = executorService;
    }

    public final void start() {
        executorService.scheduleAtFixedRate(this::onTick, 0, 1, TimeUnit.MILLISECONDS);
    }

    void onTick() {
        long minuteAgo = now.get() - MINUTE;
        writeLock.lock();
        try {
            while (!transactions.isEmpty() && transactions.peek().timestamp() < minuteAgo) {
                transactions.poll();
            }
            lastMinuteStatistics = calcStatistics();
        } finally {
            writeLock.unlock();
        }
    }

    public void acceptTransaction(Transaction transaction) {
        if (transaction.timestamp() < now.get() - MINUTE) {
            return;
        }
        writeLock.lock();
        try {
            transactions.add(transaction);
            lastMinuteStatistics = calcStatistics();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            writeLock.unlock();
        }
    }

    public DoubleSummaryStatistics lastMinuteStatistics() {
        readLock.lock();
        try {
            return lastMinuteStatistics;
        } finally {
            readLock.unlock();
        }
    }

    private DoubleSummaryStatistics calcStatistics() {
        return transactions.stream().mapToDouble(Transaction::amount).summaryStatistics();
    }
}


