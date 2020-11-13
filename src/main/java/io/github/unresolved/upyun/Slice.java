package io.github.unresolved.upyun;

/**
 * Save the file slice information
 */
class Slice {

    // X-Upyun-Part-Id starting at 0
    private int partId;

    // start position in file
    private long offset;

    // slice size
    private long length;

    // current marked position, offset is position 0
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

}
