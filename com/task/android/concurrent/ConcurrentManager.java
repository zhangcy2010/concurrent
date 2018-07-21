package com.task.android.concurrent;

import com.task.android.concurrent.Config.Business;
import java.util.ArrayList;

/**
 * 引入并发框架：
 * <p/>
 * 异步任务管理者，线程池类型与标签共同确定唯一线程池实例
 *
 * 功能：
 * 1、创建优化的线程池 （根据CPU数目和IO类型来创建优化的线程池，理论支撑是《java并发编程实战》）
 * 2、线程池内线程暂停  pause
 * 3、线程池内线程重启  resume
 * 4、线程的优先级设定  priority
 * 5、工厂类创建线程池
 * 6、写了一个扩展的AsncTask类，添加了AsncTask的优先级
 *
 * @date 2016/7/8
 */
public class ConcurrentManager {
    private static final String TAG = "ConcurrentManager";

    // 并发对外操作类的实例对象
    private static ConcurrentManager mCMInstance;

    // 线程池列表
    private ArrayList<ExThreadPoolExecutor> mThreadPoolList;

    private ConcurrentManager() {
        mThreadPoolList = new ArrayList<ExThreadPoolExecutor>();
        addThreadPool(Business.HIGH_IO, AsyncTag.IMAGE_LOADER);
    }

    /**
     * 获得并发管理类的实例
     *
     * @return
     */
    public static ConcurrentManager getInsance() {
        if (mCMInstance == null)
            synchronized (ConcurrentManager.class) {
                if (mCMInstance == null)
                    mCMInstance = new ConcurrentManager();
            }
        return mCMInstance;
    }

    /**
     * 释放管理类实例对象，防止内存泄露问题
     */
    public static synchronized void release() {
        if (null != mCMInstance) {
            mCMInstance = null;
        }
    }

    /**
     * ## 特别说明：
     * ## 此处是一个线程池来处理放入池中的所有线程，本来是处理并发的方法，
     * ## 不需要添加同步锁。
     *
     * 执行AsyncTask异步任务，参数是一个扩展的异步任务对象，如果用AsycTask来实现
     * 异步任务则需要继承该类
     *
     * @param mTask 任务
     * @param type  线程池类型
     * @param tag   线程池标签 --由type，tag决定唯一线程池实例
     */
    @SuppressWarnings({"rawtypes"})
    public void execute(ExAsyncTask mTask, Business type, String tag) {
        ExThreadPoolExecutor threadPool = initThreadPool(type, tag);
        mTask.execute(threadPool);
    }

    /**
     * 执行Runnable方法
     *
     * @param r
     */
    public void execute(Runnable r) {
        PriorityRunnable mRunnable = new PriorityRunnable(r);
        ExThreadPoolExecutor threadPool = initThreadPool(
            Business.HIGH_IO, AsyncTag.IMAGE_LOADER);
        threadPool.execute(mRunnable);
    }

    /**
     * 执行Runnable线程，不设定优先级（采用默认的优先级：5）
     *
     * @param r    Runnable
     * @param type 线程池类型
     * @param tag  线程池标签 --由type,tag决定唯一线程池实例
     */
    public void execute(Runnable r, Business type, String tag) {
        PriorityRunnable mRunnable = new PriorityRunnable(r);
        ExThreadPoolExecutor threadPool = initThreadPool(type, tag);
        threadPool.execute(mRunnable);
    }

    /**
     * 执行具有优先级的Runnable，不设定该线程的业务类型和线程池的TAG
     *
     * @param r 执行设置了优先级的Runnable
     */
    public void execute(PriorityRunnable r) {
        ExThreadPoolExecutor threadPool = initThreadPool(
            Business.HIGH_IO, AsyncTag.IMAGE_LOADER);
        threadPool.execute(r);
    }

    /**
     * 执行具有优先级的Runnable，设定该线程的业务类型和线程池的TAG
     *
     * @param r    PriorityRunnable
     * @param type 线程池类型
     * @param tag  线程池标签--由type,tag决定唯一线程池实例
     */
    public void execute(PriorityRunnable r, Business type, String tag) {
        ExThreadPoolExecutor threadPool = initThreadPool(type, tag);
        threadPool.execute(r);
    }

    /**
     * 根据异步请求初始化线程池:
     * <p/>
     * <p/>
     * 如果已经创建，则直接返回实例，
     * 若未创建，则重新创建
     *
     * @param type
     * @param mTag
     * @return
     */
    public ExThreadPoolExecutor initThreadPool(Business type, String mTag) {
        if (type == null) {
            type = Business.HIGH_IO;
        }

        if (mTag == null || mTag == "") {
            mTag = AsyncTag.IMAGE_LOADER;
        }

        if (mThreadPoolList != null) {
            for (ExThreadPoolExecutor mThreadPool : mThreadPoolList) {
                if (mThreadPool.getmThreadPoolType().equals(type)
                    && mThreadPool.getmTag().equals(mTag)) {
                    return mThreadPool;
                }
            }
        }

        return addThreadPool(type, mTag);
    }

    /**
     * 新建线程池实例，并把它添加到管理队列中
     *
     * @param type
     * @param mTag
     * @return
     */
    private ExThreadPoolExecutor addThreadPool(Business type, String mTag) {
        ExThreadPoolExecutor mThreadPool = ConcurrentFactory
            .getInstance().createThreadPollInstance(type, mTag);
        if (mThreadPoolList != null) {
            mThreadPoolList.add(mThreadPool);
        }
        return mThreadPool;
    }

    /**
     * 重要方法：暂停指定线程池，
     * 当页面滑动时，需要暂停线程池的运行，暂停线程池里运行的任务，
     * 根据Tag和type这两个维度来框定暂停哪个线程池
     *
     * @param type
     * @param mTag
     * @return
     */
    public boolean pauseThreadPool(Business type, String mTag) {
        if (type == null)
            type = Business.HIGH_IO;
        if (mTag == null || mTag == "")
            mTag = AsyncTag.IMAGE_LOADER;
        if (mThreadPoolList != null) {
            for (ExThreadPoolExecutor mThreadPool : mThreadPoolList) {
                if (mThreadPool.getmThreadPoolType().equals(type)
                    && mThreadPool.getmTag().equals(mTag)) {
                    mThreadPool.pauseExecutorService();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 重要方法：恢复指定线程池，监听页面的滑动状态，
     * <p/>
     * 如果滑动时，就调用上边的pauseThreadPool方法，
     * 当页面滑动停止时，需要重启线程池，保证池里所有线程的重新运行
     *
     * @param type
     * @param mTag
     * @return
     */
    public boolean resumeThreadPool(Business type, String mTag) {
        if (type == null)
            type = Business.HIGH_IO;
        if (mTag == null || mTag == "")
            mTag = AsyncTag.IMAGE_LOADER;
        if (mThreadPoolList != null) {
            for (ExThreadPoolExecutor mThreadPool : mThreadPoolList) {
                if (mThreadPool.getmThreadPoolType().equals(type)
                    && mThreadPool.getmTag().equals(mTag)) {
                    mThreadPool.resumeExecutorService();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 删除指定线程池
     *
     * @param type
     * @param mTag
     * @return true/false
     */
    public boolean removeThreadPool(Business type, String mTag) {
        if (type == null)
            type = Business.HIGH_IO;
        if (mTag == null || mTag == "")
            mTag = AsyncTag.IMAGE_LOADER;
        if (mThreadPoolList != null) {
            ArrayList<ExThreadPoolExecutor> tList = mThreadPoolList;
            for (ExThreadPoolExecutor mThreadPool : tList) {
                if (mThreadPool.getmThreadPoolType().equals(type)
                    && mThreadPool.getmTag().equals(mTag)) {
                    mThreadPoolList.remove(mThreadPool);
                    mThreadPool.shutdownNow();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 重要方法：当APP退出时，需要调用该方法，清除APP内所有线程池
     */
    public void clearAllThreadPool() {
        if (mThreadPoolList != null) {
            for (ExThreadPoolExecutor mThreadPool : mThreadPoolList) {
                mThreadPool.shutdownNow();
            }
        }
        mThreadPoolList = null;
    }

}