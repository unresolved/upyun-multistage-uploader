import io.github.unresolved.upyun.MultistageUploader;
import io.github.unresolved.upyun.OnProgressChangedListener;
import io.github.unresolved.upyun.UploaderConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Main {

    private static long startTime;

    public static void main(String[] args) throws FileNotFoundException {
        // 要上传的文件
        File srcFile = new File("/path/to/source/file");
        // 上传目标路径
        String destPath = URLEncoder.encode("/path/to/dest/path", StandardCharsets.UTF_8);
        // 初始化 config
        UploaderConfig config = new UploaderConfig()
                .setOperator("<operator>")
                .setBucketName("<bucketName>")
                .setPassword("<password>")
                .setSliceSize(UploaderConfig.DEFAULT_SLICE_SIZE * 10) /* 可选 */
                .setConcurrentSize(8); /* 可选 */
        // 初始化 uploader
        // 分片大小 10MB 同时上传的任务数量 16 个
        MultistageUploader uploader = new MultistageUploader(config, srcFile, destPath);
        uploader.setOnProgressChangedListener(new OnProgressChangedListener() {

            long previousTime, previousFinished;
            float bps;

            // TODO don't do any time-consuming operations in here
            public void onProgressChanged(long finished, long total) {
                // 每秒更新一次
                if (System.currentTimeMillis() - previousTime >= 1000 || finished == total) {
                    previousTime = System.currentTimeMillis();
                    bps = (float) (finished - previousFinished) / 1024 / 1024;
                    previousFinished = finished;
                }
                System.out.printf("Status: %24s (%6.2f%%) %6.2fMB/s\r", finished + "/" + total,
                        (float) finished / total * 100, bps);
            }

        });
        startTime = System.currentTimeMillis();
        // 执行上传
        uploader.upload();
        // 等待直到上传完成
        uploader.await();
        System.out.println("\n耗时： " + (System.currentTimeMillis() - startTime) / 1000 + " 秒");
    }

}

