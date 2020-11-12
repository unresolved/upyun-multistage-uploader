package io.github.unresolved.upyun;

public enum TaskStatus {

    /**
     * Task was created, but not begin to translate data.
     */
    CREATED,
    /**
     * Upload task is uploading data now
     */
    UPLOADING,
    /**
     * Upload task was finished
     */
    FINISHED,
    /**
     * Upload task was failed
     */
    FAILURE
    
}
