package io.github.unresolved.upyun;

import org.jetbrains.annotations.NotNull;

import java.net.Proxy;

public class UploaderConfig {

    /**
     * Default slice size is 1 MB (1048576 Bytes)
     */
    public static final int DEFAULT_SLICE_SIZE = 1048576;

    /**
     * Default max concurrent size
     */
    public static final int DEFAULT_CONCURRENT_SIZE = 4;

    // request timeout
    private int timeout = 30;
    // upyun endpoint
    private String endpoint = Endpoint.ED_AUTO;
    // upyun operator
    private String operator;
    // upyun operator's password
    private String password;
    // upyun bucket name
    private String bucketName;
    // http proxy
    private Proxy proxy;
    // file slice size
    private int sliceSize = DEFAULT_SLICE_SIZE;
    // concurrent upload thread count
    private int concurrentSize = DEFAULT_CONCURRENT_SIZE;
    // BufferedRandomAccessFile buffer size
    private int readBufferSize = 4096;

    public UploaderConfig() {
    }

    /**
     * Set request endpoint, it could be one of these values:<br>
     * {@link Endpoint#ED_AUTO}<br>
     * {@link Endpoint#ED_TELECOM}<br>
     * {@link Endpoint#ED_CNC}<br>
     * {@link Endpoint#ED_CTT}<br>
     *
     * @param endpoint the request endpoint
     */
    public UploaderConfig setEndpoint(@NotNull String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    /**
     * Get current endpoint.
     *
     * @return
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Set request timeout value, default is 30 seconds.
     *
     * @param seconds
     */
    public UploaderConfig setTimeout(int seconds) {
        if (seconds < 1)
            throw new IllegalArgumentException("timeout must > 0");
        this.timeout = seconds;
        return this;
    }

    /**
     * Get current request timeout value
     *
     * @return
     */
    public int getTimeout() {
        return timeout;
    }


    public String getOperator() {
        return operator;
    }

    public UploaderConfig setOperator(@NotNull String operator) {
        this.operator = operator;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UploaderConfig setPassword(@NotNull String password) {
        this.password = password;
        return this;
    }

    public String getBucketName() {
        return bucketName;
    }

    public UploaderConfig setBucketName(@NotNull String bucketName) {
        this.bucketName = bucketName;
        return this;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public UploaderConfig setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public int getSliceSize() {
        return sliceSize;
    }

    public UploaderConfig setSliceSize(int sliceSize) {
        if (sliceSize < DEFAULT_SLICE_SIZE || sliceSize > 10 * DEFAULT_SLICE_SIZE)
            throw new IllegalArgumentException("sliceSize must between 1MB and 10MB");
        if (sliceSize % DEFAULT_SLICE_SIZE != 0)
            throw new IllegalArgumentException("sliceSize must be a multiple of 1MB");
        this.sliceSize = sliceSize;
        return this;
    }

    public int getConcurrentSize() {
        return concurrentSize;
    }

    public UploaderConfig setConcurrentSize(int concurrentSize) {
        if (concurrentSize < 1)
            throw new IllegalArgumentException("concurrentSize must > 0");
        this.concurrentSize = concurrentSize;
        return this;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public UploaderConfig setReadBufferSize(int readBufferSize) {
        if (readBufferSize < 1)
            throw new IllegalArgumentException("readBufferSize must > 0");
        this.readBufferSize = readBufferSize;
        return this;
    }
}
