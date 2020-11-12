package io.github.unresolved.upyun;

/**
 * 保存文件切片信息
 */
class Slice {

    /**
     * X-Upyun-Part-Id 分块序号，序号从 0 开始
     */
    private int partId;

    /**
     * 起始位置
     */
    private long offset;

    /**
     * 切片长度
     */
    private long length;

    /**
     * current marked position
     * {@link #offset} is position 0
     */
    private long position = 0;

    public Slice(int partId, long offset, long length) {
        this.partId = partId;
        this.offset = offset;
        this.length = length;
    }

    public int getPartId() {
        return partId;
    }

    public void setPartId(int partId) {
        this.partId = partId;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "io.github.unresolved.upyun.Slice{" +
                "partId=" + partId +
                ", offset=" + offset +
                ", length=" + length +
                ", position=" + position +
                '}';
    }
    
}
