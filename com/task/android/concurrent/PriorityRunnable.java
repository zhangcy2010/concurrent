package com.task.android.concurrent;

import com.task.android.concurrent.Config.Priority;
import com.task.android.concurrent.ExAsyncTask.WorkerFutureTask;

/**
 * 具备优先级属性的Runnable,作为优先级队列的元数据
 * @date 2016/7/7
 */
public class PriorityRunnable implements Runnable, Comparable<Object> {
    private static final String TAG = "Concurrent PriorityRunnable";

    private Runnable r;

    // 默认的优先级为5
    public int mPriority = 5;

    public PriorityRunnable(Runnable r) {
        this.r = r;
    }

    /**
     * 带优先级的Runnable的构造方法
     *
     * @param r
     * @param priority
     */
    public PriorityRunnable(Runnable r, Priority priority) {
        this.r = r;
        if (priority == null)
            priority = Priority.NORM_PRIORITY;
        switch (priority) {
            // 最小优先级为10
            case MIN_PRIORITY:
                mPriority = 10;
                break;
            // 普通的优先级为5
            case NORM_PRIORITY:
                mPriority = 5;
                break;
            // 最大的优先级为1
            case MAX_PRIORITY:
                mPriority = 1;
                break;
        }
    }

    /**
     * 获得优先级的值
     *
     * @return mPriority
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * 给外部提供了一个接口来设置异步任务的优先级
     *
     * @param priority
     */
    public void setmPriority(Priority priority) {
        if (priority == null)
            priority = Priority.NORM_PRIORITY;
        switch (priority) {
            case MIN_PRIORITY:
                mPriority = 10;
                break;
            case NORM_PRIORITY:
                mPriority = 5;
                break;
            case MAX_PRIORITY:
                mPriority = 1;
                break;
        }
    }

    @Override
    public void run() {
        this.r.run();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public int compareTo(Object another) {
        if (another instanceof WorkerFutureTask) {
            return this.mPriority < ((WorkerFutureTask) another).mPriority ? -1
                : this.mPriority > ((WorkerFutureTask) another).mPriority ? 1 : 0;
        } else if (another instanceof PriorityRunnable) {
            return this.mPriority < ((PriorityRunnable) another).mPriority ? -1
                : this.mPriority > ((PriorityRunnable) another).mPriority ? 1 : 0;
        } else
            return 0;
    }

}
