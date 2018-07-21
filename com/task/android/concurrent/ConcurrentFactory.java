package com.task.android.concurrent;

import android.util.Log;

import com.task.android.concurrent.Config.Business;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工厂，创建三种业务类型的线程池
 *
 * @date 2016/7/8
 */
public class ConcurrentFactory {
    private static final String TAG = "ConcurrentFactory";
    public static final int MAX_CORE_SIZE = 3;

    private static int mAvailableProcessors = Runtime.getRuntime().availableProcessors();

    private static class ThreadPoolFactoryHolder {
        private final static ConcurrentFactory instance = new ConcurrentFactory();
    }

    /**
     * 获得并发工厂的实例
     *
     * @return
     */
    public static ConcurrentFactory getInstance() {
        return ThreadPoolFactoryHolder.instance;
    }

    /**
     * <p/>
     * 新建线程池实例 Business枚举和Tag唯一确定一个线程池
     * 如果没有符合线程池实例，则会新建一个，并加入到ConcurrentManager的链表中加以管理
     * <p/>
     * <p/>
     * Business的分类依据CPU等待时间的长短（反而言之即CPU负载的大小）
     * CPU等待时间是指CPU在完成某项运算时，由于缺少运算的数据而处于空闲状态的时间
     * 一般而言等待时间越长的任务，处于空闲状态的时间越长，可以腾出手处理其它任务 本质就是cpu执行速度远大于硬盘读写速度更远大于远程回来数据的速度
     * <p/>
     * 普通操作可以放在CPU 本地操作可以放在HIGH_IO 远程操作可以放在LOW_IO
     * <p/>
     * Tag可以根据客户端的任务逻辑随意编写，不写则为“default” 不同Tag的任务因分处不同池，故不会产生竞争条件
     * Tag可以用来作为整理操作的某种依据（譬如pause/resume/schedule/delay）
     *
     * @param type
     * @param tag
     * @return mThreadPool
     */
    public ExThreadPoolExecutor createThreadPollInstance(Business type,
                                                         String tag) {
        Log.d(TAG , "availableProcessors:" + mAvailableProcessors);

        ExThreadPoolExecutor mThreadPool;
        if (type == null) {
            type = Business.HIGH_IO;
        }

        if (tag == null) {
            tag = AsyncTag.IMAGE_LOADER;
        }

        switch (type) {
            case CPU:
                final int cpuThreadNums = mAvailableProcessors + 1;
                Log.d(TAG ,  "cpuThreadNums:" + cpuThreadNums);

                mThreadPool = new ExThreadPoolExecutor(cpuThreadNums, cpuThreadNums, 0L,
                        TimeUnit.MILLISECONDS,
                        new PriorityBlockingQueue<Runnable>(), type, tag);
                break;
            case HIGH_IO:
                int highIOThreadNums = (int) Math.ceil(mAvailableProcessors * 1.1);
                highIOThreadNums = Math.min(highIOThreadNums, MAX_CORE_SIZE);

                Log.d(TAG ,  "highIOThreadNums:" + highIOThreadNums);

                mThreadPool = new ExThreadPoolExecutor(highIOThreadNums, highIOThreadNums, 0L,
                        TimeUnit.MILLISECONDS,
                        new PriorityBlockingQueue<Runnable>(), type, tag);
                break;
            case LOW_IO:
                int lowIOThreadNums = (int) Math.ceil(mAvailableProcessors * 1.5);
                lowIOThreadNums = Math.min(lowIOThreadNums, MAX_CORE_SIZE);

                Log.d(TAG ,  "lowIOThreadNums:" + lowIOThreadNums);

                mThreadPool = new ExThreadPoolExecutor(lowIOThreadNums, lowIOThreadNums, 0L,
                        TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(10),
                        type, tag);
                mThreadPool
                        .setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
                break;
            default:
                mThreadPool = new ExThreadPoolExecutor(5, 5, 0L,
                        TimeUnit.MILLISECONDS,
                        new PriorityBlockingQueue<Runnable>(), type, tag);
                break;
        }

        return mThreadPool;
    }
}