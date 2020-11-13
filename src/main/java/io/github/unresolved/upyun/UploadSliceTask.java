package io.github.unresolved.upyun;

import okhttp3.*;
import okio.BufferedSink;

import java.io.File;
import java.io.IOException;

class UploadSliceTask implements Runnable {

    private final MultistageUploader uploader;
    private static int nextTaskId = 1;
    private final int taskId;
    private final long createTime;
    private final File targetFile;
    private final Slice targetSlice;
    // X-Upyun-Multi-Uuid generated when 'initiate' stage
    private final String upyunMultiUuid;
    // current task status
    private TaskStatus taskStatus = TaskStatus.CREATED;
    private final OkHttpClient mClient;

    public UploadSliceTask(MultistageUploader uploader, File targetFile, Slice targetSlice, String upyunMultiUuid) {
        this.uploader = uploader;
        this.mClient = uploader.getOkHttpClient();
        this.taskId = UploadSliceTask.nextTaskId++;
        this.targetFile = targetFile;
        this.targetSlice = targetSlice;
        this.upyunMultiUuid = upyunMultiUuid;
        this.createTime = System.currentTimeMillis();
    }

    public void run() {
        try {
            // buffer size 4K
            final int bufferSize = 4096;
            // open target file
            final BufferedRandomAccessFile file = new BufferedRandomAccessFile(targetFile, "r", bufferSize);
            // seek to offset in file
            file.seek(targetSlice.getOffset());

            UploaderConfig config = uploader.getConfig();

            String uploadDate = UpyunUtils.getGMTDate();
            String uriPath = "/" + config.getBucketName() + "/" + uploader.getDestPath();
            String uploadSign = UpyunUtils.sign("PUT", uploadDate, uriPath, config.getOperator(),
                    UpyunUtils.md5(config.getPassword()), null);
            if (uploadSign == null)
                throw new UploaderException("failed to calculate multistage upload signature.");

            RequestBody requestBody = new RequestBody() {

                long remaining = targetSlice.getLength();

                @Override
                public MediaType contentType() {
                    return null;
                }

                @Override
                public long contentLength() {
                    return remaining;
                }

                public void writeTo(BufferedSink bufferedSink) {
                    try {
                        while (remaining != 0) {
                            // read from disk
                            byte[] buffer = new byte[bufferSize];
                            int readCount = file.read(buffer, 0, buffer.length);
                            // write to bufferedSink
                            bufferedSink.write(buffer, 0, readCount);
                            remaining -= readCount;
                            targetSlice.setPosition(targetSlice.getPosition() + readCount);
                            // update global upload progress
                            uploader.notifyProgressChanged();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        // mark this task failure
                        taskStatus = TaskStatus.FAILURE;
                    } finally {
                        try {
                            file.close();
                            bufferedSink.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            Request.Builder builder = new Request.Builder()
                    .url(config.getEndpoint() + uriPath)
                    .header("Date", uploadDate)
                    .header("Authorization", uploadSign)
                    .header("User-Agent", UpyunUtils.USER_AGENT)
                    .header("X-Upyun-Multi-Stage", "upload")
                    .header("X-Upyun-Multi-Uuid", upyunMultiUuid)
                    .header("X-Upyun-Part-Id", String.valueOf(targetSlice.getPartId()))
                    .header("Content-Length", String.valueOf(targetSlice.getLength()))
                    .method("PUT", requestBody);

            // mark this task is uploading
            taskStatus = TaskStatus.UPLOADING;
            Response response = mClient.newCall(builder.build()).execute();
            ResponseBody responseBody = response.body();
            if (response.code() != 204) {
                throw new UploaderException("excepted response code: " + response.code() + ", server responded: "
                        + (responseBody == null ? null : responseBody.string()));
            }
            // mark this task is finished
            taskStatus = TaskStatus.FINISHED;
        } catch (UploaderException | IOException e) {
            e.printStackTrace();
        }
    }

    public int getTaskId() {
        return taskId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public Slice getTargetSlice() {
        return targetSlice;
    }

    public String getUpyunMultiUuid() {
        return upyunMultiUuid;
    }

    public TaskStatus getStatus() {
        return taskStatus;
    }
}
