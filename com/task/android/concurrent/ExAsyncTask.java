package com.task.android.concurrent;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.task.android.concurrent.Config.Priority;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 扩展的AsyncTask抽象类，是根据Android原生的AsyncTask扩展的一个新类，
 * 主要是添加了对任务的优先级的设定，该方式设定的线程优先级是生效的。
 *
 * <p/>
 * 如果项目中有使用AsyncTask的地方，需要子类继承于该类而不继承于原生的AsyncTask类
 *
 * @param <Params>
 * @param <Progress>
 * @param <Result>
 * @date 2016/7/8
 */
public abstract class ExAsyncTask<Params, Progress, Result> {
    private static final String TAG = "Concurrent ExAsyncTask";

    private static final int MESSAGE_POST_RESULT = 0x1;
    private static final int MESSAGE_POST_PROGRESS = 0x2;
    private static final int MESSAGE_POST_CANCEL = 0x3;

    private static final InternalHandler mHandler = new InternalHandler();

    private final WorkerRunnable<Params, Result> mWorker;
    private final FutureTask<Result> mFuture;

    private volatile Status mStatus = Status.PENDING;
    private int mPriority = 5;
    Params[] mParams;

    /**
     * Indicates the current status of the task. Each status will be set only
     * once during the lifetime of a task.
     */
    public enum Status {
        /**
         * Indicates that the task has not been executed yet.
         */
        PENDING,
        /**
         * Indicates that the task is running.
         */
        RUNNING,
        /**
         * Indicates that {@link AsyncTask#onPostExecute} has finished.
         */
        FINISHED,
    }

    /**
     * @hide Used to force static handler to be created.
     */
    public static void init() {
        mHandler.getLooper();
    }

    public ExAsyncTask() {
        mWorker = new WorkerRunnable<Params, Result>() {

            public Result call() throws Exception {
                android.os.Process.setThreadPriority(mPriority);
                return doInBackground(mParams);
            }
        };

        mFuture = new WorkerFutureTask(mWorker) {
            @SuppressWarnings("unchecked")
            @Override
            protected void done() {
                Message message;
                Result result = null;
                try {
                    result = get();
                } catch (InterruptedException e) {
                    Log.d(TAG ,  " InterruptedException = " + e.getMessage());
                } catch (ExecutionException e) {
                    Log.d(TAG , " ExecutionException = " + e.getMessage());
                    //throw new RuntimeException(
                    //        "An error occured while executing doInBackground()",
                    //        e.getCause());
                } catch (CancellationException e) {
                    message = mHandler.obtainMessage(MESSAGE_POST_CANCEL,
                        new AsyncTaskResult<Result>(ExAsyncTask.this,
                            (Result[]) null));
                    message.sendToTarget();
                    return;
                } catch (Throwable t) {
                    Log.d(TAG , " Throwable = " + t.getMessage());
                    //throw new RuntimeException(
                    //        "An error occured while executing "
                    //          + "doInBackground()", t);
                }

                message = mHandler.obtainMessage(MESSAGE_POST_RESULT,
                    new AsyncTaskResult<Result>(ExAsyncTask.this,
                        result));
                message.sendToTarget();
            }

            @SuppressWarnings("unchecked")
            @Override
            public int compareTo(Object another) {
                if (another instanceof ExAsyncTask.WorkerFutureTask) {
                    return this.mPriority < ((WorkerFutureTask) another).mPriority ? -1
                        : this.mPriority > ((WorkerFutureTask) another).mPriority ? 1
                        : 0;
                } else if (another instanceof PriorityRunnable) {
                    return this.mPriority < ((PriorityRunnable) another).mPriority ? -1
                        : this.mPriority > ((PriorityRunnable) another).mPriority ? 1
                        : 0;
                } else {
                    return 0;
                }
            }
        };
    }

    private int getmPriority() {
        return mPriority;
    }

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
            default:
                mPriority = 5;
                break;
        }
    }

    private Params[] getmParams() {
        return mParams;
    }

    public void setmParams(Params[] mParams) {
        this.mParams = mParams;
    }

    public final Status getStatus() {
        return mStatus;
    }

    protected abstract Result doInBackground(Params... params);

    protected void onPreExecute() {
    }

    protected void onPostExecute(Result result) {
    }

    protected void onProgressUpdate(Progress... values) {
    }

    protected void onCancelled() {
    }

    public final boolean isCancelled() {
        return mFuture.isCancelled();
    }

    public final boolean cancel(boolean mayInterruptIfRunning) {
        return mFuture.cancel(mayInterruptIfRunning);
    }

    public final Result get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    public final Result get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    /**
     * 需要额外传入一个{@link ExThreadPoolExecutor} 参数，调用方法如下：
     * <p/>
     * <pre class="prettyprint">
     * AsyncTaskCenter&lt;Integer, Void, Void&gt; task = new AsyncTaskCenter&lt;Integer, Void, Void&gt;() {
     * <p/>
     * protected Void doInBackground(Integer... mParams) {
     * return null;
     * }
     * <p/>
     * };
     * Void[] mParams = { };
     * task.setParams(mParams);
     * ExThreadPoolExecutor executor= new ExThreadPoolExecutor(...);
     * task.execute(executor);
     * </pre>
     *
     * @param executor 传入一个{@link ExThreadPoolExecutor}
     * @return
     */
    public final ExAsyncTask<Params, Progress, Result> execute(
        ExThreadPoolExecutor executor) {
        if (mStatus != Status.PENDING) {
            switch (mStatus) {
                case RUNNING:
                    throw new IllegalStateException("Cannot execute task:"
                        + " the task is already running.");
                case FINISHED:
                    throw new IllegalStateException("Cannot execute task:"
                        + " the task has already been executed "
                        + "(a task can be executed only once)");
                default:
                    break;
            }
        }

        mStatus = Status.RUNNING;

        onPreExecute();

        mWorker.mParams = this.getmParams();
        mWorker.mPriority = this.getmPriority();

        executor.execute(mFuture);

        return this;
    }

    protected final void publishProgress(Progress... values) {
        mHandler.obtainMessage(MESSAGE_POST_PROGRESS,
            new AsyncTaskResult<Progress>(this, values)).sendToTarget();
    }

    private void finish(Result result) {
        onPostExecute(result);
        mStatus = Status.FINISHED;
    }

    private static class InternalHandler extends Handler {
        @SuppressWarnings({"unchecked"})
        @Override
        public void handleMessage(Message msg) {
            AsyncTaskResult<?> result = (AsyncTaskResult<?>) msg.obj;
            switch (msg.what) {
                case MESSAGE_POST_RESULT:
                    // There is only one result
                    result.mTask.finish(result.mData[0]);
                    break;
                case MESSAGE_POST_PROGRESS:
                    result.mTask.onProgressUpdate(result.mData);
                    break;
                case MESSAGE_POST_CANCEL:
                    result.mTask.onCancelled();
                    break;
                default:
                    break;
            }
        }
    }

    private static abstract class WorkerRunnable<Params, Result> implements
        Callable<Result> {
        Params[] mParams;
        int mPriority = android.os.Process.THREAD_PRIORITY_BACKGROUND;
    }

    public abstract class WorkerFutureTask extends FutureTask<Result> implements
        Comparable<Object> {

        int mPriority;

        @SuppressWarnings({"rawtypes", "unchecked"})
        public WorkerFutureTask(WorkerRunnable callable) {
            super(callable);
            mPriority = callable.mPriority;
        }
    }

    @SuppressWarnings("rawtypes")
    private static class AsyncTaskResult<Data> {
        final ExAsyncTask mTask;
        final Data[] mData;

        AsyncTaskResult(ExAsyncTask task, Data... data) {
            mTask = task;
            mData = data;
        }
    }

}
