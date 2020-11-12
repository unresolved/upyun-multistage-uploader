package io.github.unresolved.upyun;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MultistageUploader {

    private final UploaderConfig config;
    private final File targetFile;
    private final String destPath;
    private ExecutorService threadPool;
    // queue contains all slice upload tasks
    private final LinkedList<UploadSliceTask> taskQueue = new LinkedList<>();
    private OkHttpClient mClient;
    private OnProgressChangedListener listener;

    /**
     * @param config     uploader config must be not null.
     * @param targetFile file must accessible.
     * @param destPath   destination path should be not null.
     */
    public MultistageUploader(@NotNull UploaderConfig config, @NotNull File targetFile, @NotNull String destPath)
            throws FileNotFoundException {

        if (!targetFile.exists())
            throw new FileNotFoundException(targetFile.getName());
        // if the first character is '/', remote it
        if (destPath.charAt(0) == '/')
            destPath = destPath.substring(1);
        // destination path should be not null
        if (destPath.equals(""))
            throw new IllegalArgumentException("invalid destination path.");

        this.config = config;
        this.targetFile = targetFile;
        this.destPath = destPath;
    }

    /**
     * Start upload progress
     */
    public void upload() {
        try {
            long fileLength = targetFile.length();
            // get upyun multi stage uuid
            mClient = new OkHttpClient.Builder()
                    .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                    .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                    .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                    .proxy(config.getProxy())
                    .build();

            String uriPath = "/" + config.getBucketName() + "/" + destPath;

            String initiateDate = UpyunUtils.getGMTDate();
            String initiateSign = UpyunUtils.sign("PUT", initiateDate, uriPath, config.getOperator(),
                    UpyunUtils.md5(config.getPassword()), null);
            if (initiateSign == null) {
                throw new UploaderException("failed to calculate multistage upload initiate signature.");
            }

            Request request = new Request.Builder()
                    .url(config.getEndpoint() + uriPath)
                    .header("Date", initiateDate)
                    .header("Authorization", initiateSign)
                    .header("User-Agent", UpyunUtils.USER_AGENT)
                    .header("X-Upyun-Multi-Disorder", "true")
                    .header("X-Upyun-Multi-Stage", "initiate")
                    .header("X-Upyun-Multi-Length", String.valueOf(fileLength))
                    .header("X-Upyun-Multi-Part-Size", String.valueOf(config.getSliceSize()))
                    .method("PUT", RequestBody.create("", null))
                    .build();
            Response response = mClient.newCall(request).execute();
            if (response.code() != 204) {
                ResponseBody body = response.body();
                throw new UploaderException("excepted response code: " + response.code() + ", server responded: "
                        + (body == null ? null : body.string()));
            }
            String upyunMultiUuid = response.header("X-Upyun-Multi-Uuid");
            if (upyunMultiUuid == null)
                throw new UploaderException("server responded 204 but can not find X-Upyun-Multi-Uuid in response headers.");
            response.close();

            threadPool = Executors.newFixedThreadPool(config.getConcurrentSize());
            long remaining = fileLength;
            int partId = 0;
            // allocate all tasks
            while (remaining != 0) {
                // calculate slice size
                int sliceSize = config.getSliceSize();
                if (remaining < config.getSliceSize())
                    sliceSize = (int) remaining; // range: [1, DEFAULT_SLICE_SIZE)
                // create a new file slice
                Slice slice = new Slice(partId++, fileLength - remaining, sliceSize);
                // create a new upload task
                UploadSliceTask task = new UploadSliceTask(this, targetFile, slice, upyunMultiUuid);
                // offer task to task queue
                taskQueue.add(task);
                threadPool.submit(task);
                // slice allocated, decrease remaining value
                remaining -= sliceSize;
            }
            new Thread(() -> {
                Thread.currentThread().setName("Upyun Multistage Uploader Task Monitor Thread");
                while (true) {
                    int finishedCount = 0;
                    for (int i = 0; i < taskQueue.size(); i++) {
                        UploadSliceTask targetTask = taskQueue.get(i);
                        if (targetTask.getStatus() == TaskStatus.FINISHED)
                            finishedCount++;
                        // 失败的任务重新添加到任务列表最后面
                        if (targetTask.getStatus() == TaskStatus.FAILURE) {
                            taskQueue.remove(i);
                            targetTask.getTargetSlice().setPosition(0);
                            threadPool.submit(targetTask);
                            taskQueue.add(targetTask);
                            break;
                        }
                    }
                    if (finishedCount == taskQueue.size())
                        break;
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    // complete upyun multistage upload
                    String completeDate = UpyunUtils.getGMTDate();
                    String completeSign = UpyunUtils.sign("PUT", completeDate, uriPath, config.getOperator(),
                            UpyunUtils.md5(config.getPassword()), null);
                    if (completeSign == null) {
                        throw new UploaderException("failed to calculate multistage upload complete signature.");
                    }

                    Request completeRequest = new Request.Builder()
                            .url(config.getEndpoint() + uriPath)
                            .header("Date", completeDate)
                            .header("Authorization", completeSign)
                            .header("X-Upyun-Multi-Stage", "complete")
                            .header("X-Upyun-Multi-Uuid", upyunMultiUuid)
                            .header("Content-Length", "0")
                            .method("PUT", RequestBody.create("", null))
                            .build();

                    Response completeResponse = mClient.newCall(completeRequest).execute();
                    ResponseBody responseBody = completeResponse.body();
                    if (response.code() != 201 && response.code() != 204) {
                        throw new UploaderException("excepted response code: " + response.code() + ", server responded: "
                                + (responseBody == null ? null : responseBody.string()));
                    }
                    completeResponse.close();
                } catch (IOException | UploaderException e) {
                    e.printStackTrace();
                } finally {
                    if (threadPool != null && !threadPool.isShutdown())
                        threadPool.shutdownNow();
                }

            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set a progress listener, uploader will call this listener when upload progress is updated.
     *
     * @param listener the listener uploader will call
     */
    public void setOnProgressChangedListener(OnProgressChangedListener listener) {
        this.listener = listener;
    }

    /**
     * Block until all tasks have completed, or time past {@link Long#MAX_VALUE} days.
     *
     * @see #await(long, TimeUnit)
     */
    public boolean await() {
        return await(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    /**
     * @param milliseconds wait for milliseconds
     * @see #await(long, TimeUnit)
     */
    public boolean await(long milliseconds) {
        return await(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Blocks until all tasks have completed, or the timeout occurs。
     *
     * @return true if all tasks have completed and false if the timeout elapsed before termination
     */
    public boolean await(long timeout, TimeUnit unit) {
        try {
            return threadPool.awaitTermination(timeout, unit);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public UploaderConfig getConfig() {
        return config;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public String getDestPath() {
        return destPath;
    }


    protected void notifyProgressChanged() {
        if (listener != null) {
            long finished = 0;
            long total = 0;
            for (UploadSliceTask task : taskQueue) {
                if (task.getStatus() != TaskStatus.FAILURE) {
                    finished += task.getTargetSlice().getPosition();
                    total += task.getTargetSlice().getLength();
                }
            }
            listener.onProgressChanged(finished, total);
        }
    }

    protected OkHttpClient getOkHttpClient() {
        return mClient;
    }
}
