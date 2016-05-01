/**
 * Copyright 2016 Matthias Düsterhöft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.matdue.isk.download;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pool which pools a maximum number of threads.
 * A typical usage is downloading several URLs at the same time, using {@link DownloadPool}.
 * Dispose this pool by calling {@link #shutdown(long)} after usage to free up any resources.
 * <p></p>
 * Threads created by this pool are named <code>DownloadPool #n</code> with minimum priority.
 */
public class DownloadPool {

    private ExecutorService _pool;

    /**
     * Creates a pool with up to 6 threads. 6 is the typical number of concurrent connections
     * browsers use to download resources.
     */
    public DownloadPool() {
        this(6);
    }

    /**
     * Creates a pool with up to <code>nThreads</code> concurrent threads.
     *
     * @param nThreads maximum number of concurrent threads.
     */
    public DownloadPool(int nThreads) {
        _pool = Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
            private final AtomicInteger _count = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DownloadPool #" + _count.getAndIncrement());
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        });
    }

    /**
     * Adds a download task. If the number of concurrently running tasks
     * has already reached its maximum, the additional task will be queued.
     * The queue is unlimited.
     *
     * @param command new task.
     */
    public void execute(Runnable command) {
        _pool.execute(command);
    }

    /**
     * Shuts down this pool and wait up to <code>timeout</code> seconds
     * until all running tasks have finished. Any unfinished tasks will be halted.
     *
     * @param timeout seconds to wait until all tasks have finished.
     */
    public void shutdown(long timeout) {
        _pool.shutdown();
        try {
            _pool.awaitTermination(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.w("DownloadPool", e);
        }

        _pool.shutdownNow();
    }
}
