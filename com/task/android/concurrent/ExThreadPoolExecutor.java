package com.task.android.concurrent;

import com.task.android.concurrent.Config.Business;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 扩展的线程池，增加了暂停和重启线程池的功能
 *
 * @date 2016/7/9
 */
public class ExThreadPoolExecutor extends ThreadPoolExecutor {
    private static final String TAG = "ExThreadPoolExecutor";

    // 暂停和重启锁
    private PauseAndResumeLock mCont;
    // 线程池的业务类型
    private Business mThreadPoolType = Business.HIGH_IO;
    // 线程池默认的TAG
    private String mTag = AsyncTag.IMAGE_LOADER;
    // 线程Group
    private final ThreadGroup mThreadGroup;
    // 线程工厂
    private final ThreadFactory mThreadFactory;

    /**
     * 扩展的线程池构造函数
     *
     * @param corePoolSize
     * @param maximumPoolSize
     * @param keepAliveTime
     * @param unit
     * @param workQueue
     * @param mThreadPoolType
     * @param mTag
     */
    public ExThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                long keepAliveTime, TimeUnit unit,
                                BlockingQueue<Runnable> workQueue, final Business mThreadPoolType,
                                final String mTag) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.mThreadPoolType = mThreadPoolType;
        this.mTag = mTag;
        this.mCont = new PauseAndResumeLock();

        mThreadGroup = new ThreadGroup("Group # " + mThreadPoolType.name());
        mThreadFactory = new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(mThreadGroup, r, mThreadPoolType.name()
                    + " # " + mTag + "#" + mCount.getAndIncrement());
            }
        };

        setThreadFactory(mThreadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        try {
            this.mCont.checkIn();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.beforeExecute(t, r);
    }

    /**
     * 恢复复线程池的运行
     */
    public void resumeExecutorService() {
        this.mCont.resume();
    }

    /**
     * 暂停线程池
     */
    public void pauseExecutorService() {
        this.mCont.pause();
    }

    /**
     * 获得线程池的业务Type
     *
     * @return mThreadPoolType
     */
    public Business getmThreadPoolType() {
        return mThreadPoolType;
    }

    /**
     * 获得线程池TAG
     *
     * @return mTag
     */
    public String getmTag() {
        return mTag;
    }
}
