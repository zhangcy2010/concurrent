package com.task.android.concurrent;

import java.io.Serializable;

/**
 * 并发框架的工具类，设置并发的IO业务类型、优先级和线程池的TAG标记
 *
 * @date 2016/7/7
 */
public class Config {
    private static final String TAG = "Config";

    /**
     * CPU级别的IO: 密集运算/本地计算任务
     * <p/>
     * HIGH_IO: 本地操作/文件/DB/缓存读写，例如：读Cache是高IO
     * <p/>
     * LOW_IO: 网络访问/进程间通信
     */
    public enum Business implements Serializable {
        CPU, HIGH_IO, LOW_IO;
    }

    /**
     * 线程的优先级定级
     * <p/>
     * NORM:普通优先级 5
     * <p/>
     * MIN:低优先级 10
     * <p/>
     * MAX:高优先级 1
     */
    public enum Priority implements Serializable {
        NORM_PRIORITY, MIN_PRIORITY, MAX_PRIORITY;
    }

}
