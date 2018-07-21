package com.task.android.concurrent;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程的暂停和恢复锁实现类
 *
 * @date 2016/7/7
 */
public class PauseAndResumeLock {
    private static final String TAG = "PauseAndResumeLock";

	// 是否暂停的Flag
	private boolean isPaused;
	// ReentrantLock
	private ReentrantLock pauseLock = new ReentrantLock();
	// Condition
	private Condition unpaused = pauseLock.newCondition();

	public PauseAndResumeLock() {
		super();
	}

	public void checkIn() throws InterruptedException {
		if (isPaused) {
			pauseLock.lock();
			try {
				while (isPaused) {
					unpaused.await();
				}
			} finally {
				pauseLock.unlock();
			}
		}
	}

	/**
	 * 暂停
	 */
	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	/**
	 * 重启
	 */
	public void resume() {
		pauseLock.lock();
		try {
			if (isPaused) {
				isPaused = false;
				unpaused.signalAll();
			}
		} finally {
			pauseLock.unlock();
		}
	}
}