![](images/topic.jpg)

# upyun-multistage-uploader

![](https://img.shields.io/github/license/unresolved/upyun-multistage-uploader) ![](https://img.shields.io/github/v/release/unresolved/upyun-multistage-uploader)

## 介绍

upyun-multistage-uploader 是一个基于又拍云并行式断点续传 REST API 的大文件上传解决方案。

使用此 SDK 你可以轻松快速地上传一个大文件到又拍云对象存储。

## 用法

使用此 SDK 无需关心过多底层细节，只需简单几步就可以上传文件。

### 准备欲上传的文件

通过实例化一个 File 对象来指定将要被用来上传的文件。

> 注意：该文件必须存在，且程序有权限读取该文件的数据，否则会抛出 `FileNotFoundException` 异常或 `IOException` 异常。

```java
File srcFile = new File("/path/to/source/file");
```

### 定义一个要上传到又拍云对象储存的位置

这里使用了 UTF-8 编码将路径进行了 URL 编码。

> 注意：请使用确保该路径已经被URL编码，否则将出现签名计算错误或请求无法上传。

```java
String destPath = URLEncoder.encode("/path/to/destination", StandardCharsets.UTF_8);
```

### 初始化 UploaderConfig 对象

UploaderConfig 类是用来对 MultistageUploader 类进行一系列配置的。

该类提供了 MultistageUploader 类所需的一切配置。

#### 设置 Endpoint（可选）

Endpoint 是指又拍云对象存储的 API 访问链接。

```java
public UploaderConfig setEndpoint(@NotNull String endpoint);
```

可以调用 Endpoint 类中的常量来快速精准设置 Endpoint 值。

> ED_AUTO：根据当前网络自动确定路线。
>
> ED_TELECOM： 电信路线。
>
> ED_CNC： 联通路线。
>
> ED_CTT： 移动路线。

```java
public class Endpoint {
    public static final String ED_AUTO = "https://v0.api.upyun.com";
    public static final String ED_TELECOM = "https://v1.api.upyun.com";
    public static final String ED_CNC = "https://v2.api.upyun.com";
    public static final String ED_CTT = "https://v3.api.upyun.com";
}
```

#### 设置 Operator

Operator 是指又拍云对象存储的操作员名称。

```java
public UploaderConfig setOperator(@NotNull String operator);
```

#### 设置 Password

Password 是指又拍云对象存储的访问密码。

```java
public UploaderConfig setPassword(@NotNull String password);
```

#### 设置 BucketName

BucketName 是指又拍云对象存储的空间名称。

```java
public UploaderConfig setBucketName(@NotNull String bucketName);
```

#### 设置代理（可选）

可以指定一个代理来使 MultistageUploader 通过该代理上传文件。

```java
public UploaderConfig setProxy(Proxy proxy);
```

#### 设置分片大小（可选）

MultistageUploader 会将一个大文件分割成一个个块来分别进行上传。

分片大小单位是字节，必须是 1MB 的整数倍，最小允许 1MB 每个分块，最大允许 10MB 每个分块。

默认分片大小在 `UploaderConfig` 中常量 `DEFAULT_SLICE_SIZE` 有定义。

如果给定的分片大小不符合规定的范围则会抛出 `IllegalArgumentException` 异常。

```java
public UploaderConfig setSliceSize(int sliceSize);
```

#### 设置上传并发数（可选）

上传并发数指最多同时进行的上传连接数。

> 并发数最低不能低于 1 默认为 4 建议不要设置的太高根据上行带宽进行适当调整。

```java
public UploaderConfig setConcurrentSize(int concurrentSize);
```

#### 设置磁盘读取缓存（可选）

MultistageUploader 每一个分块上传任务都会独立在自己的线程内进行磁盘读取操作

如果不设置缓存那么每一次进行数据读取操作系统都会去调度真实的磁盘读操作，这是比较耗时的非常影响程序性能。所以设置一个缓存就可以在每次读取磁盘时多读取指定长度的数据，下次读取数据时只要数据还在缓存内操作系统就不需要调度真实的磁盘读操作，这大大地提高了程序性能。

> 缓存长度不能小于 1 否则会抛出 `IllegalArgumentException` 异常。
>
> 缓存长度默认为 4096 字节，建议不要设置的过小或过大，过小会导致缓存无法发挥 其优势，过大会导致内存资源占用过多。

```java
public UploaderConfig setReadBufferSize(int readBufferSize);
```

### 初始化 MultistageUploader 对象

MultistageUploader 是核心对象，用于文件上传。

```java
public MultistageUploader(@NotNull UploaderConfig config, @NotNull File targetFile, @NotNull String destPath) throws FileNotFoundException;
```

构造器需要 3 个参数，分别是 UploaderConfig 对象、 欲上传的文件对象、上传到的路径字符串。

### 设置上传回调（可选）

通过 SDK 提供的 OnProgressChangedListener 类你可以轻松得知当前的上传进度。

方法 onProgressChanged 含有 2 个参数分别对应已经上传的字节数和总字节数（等同于文件大小）。

```java
public interface OnProgressChangedListener {
    void onProgressChanged(long finished, long total);
}
```

### 上传文件

只需调用一个方法 MultistageUploader 就会自动进行任务分配，文件上传等操作。

该方法调用后会立即返回。

```java
public void upload();
```

### 等待文件上传完成

MultistageUploader 提供了堵塞等待文件上传完成的方法。

```java
// 直到文件上传前一直等待
public boolean await();
// 等待参数值毫秒后返回（如果返回值为 true 则文件上传完成或发生了无法进行重传的异常，如果为 false 则文件上传还未完成）
public boolean await(long milliseconds); 
// 等待参数数值给定时间单位时间长度后返回（如果返回值为 true 则文件上传完成或发生了无法进行重传的异常，如果为 false 则文件上传还未完成）
public boolean await(long timeout, TimeUnit unit); 
```

## 示例

该示例演示了完整的流程，提供了实时上传进度显示功能以及上传速度计算。

>  示例文件所在位置：src/test/java/Main.java

```java
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
                .setOperator("<operator>") // 此处设置 Operator
                .setPassword("<password>") // 此处设置 Password
                .setBucketName("<bucketName>") // 此处设置 BucketName
                .setSliceSize(UploaderConfig.DEFAULT_SLICE_SIZE * 10) /* （可选）默认为 1MB */
                .setConcurrentSize(8); /* （可选）默认为 4 线程*/
                .setReadBufferSize(8 * 1024); /* （可选）默认为 4096 字节 */
        // 初始化 uploader
        MultistageUploader uploader = new MultistageUploader(config, srcFile, destPath);
        // 设置上传回调
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
        // 等待直到上传完成或出现异常
        uploader.await();
        // 输出上传花费的时间
        System.out.println("\n耗时： " + (System.currentTimeMillis() - startTime) / 1000 + " 秒");
    }

}
```

## 相关资料

又拍云对象存储文档：https://help.upyun.com/docs/storage/

又拍云对象存储 REST API 文档：https://help.upyun.com/knowledge-base/rest_api/

又拍云对象存储错误码对照表：https://help.upyun.com/knowledge-base/errno/