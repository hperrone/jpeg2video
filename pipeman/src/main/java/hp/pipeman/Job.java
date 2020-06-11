package hp.pipeman;

import java.io.File;

import javax.json.JsonObject;

/**
 * @brief Encoding Job data object
 */
class Job {

    public static enum JobStatus {
        STOPPED,     /**< Job is stopped, not yet started     */
        RUNNING,     /**< Encoding is on going                */
        COMPLETED,   /**< Encoding completed successfuly      */
        ERROR        /**< Encoding failed                     */
    };

    /** The job identification                                */
    private final int         id;

    /** Directory with the image sequence to encode           */
    private final File        in;

    /** Image sequence frame rate (frames per second)         */
    private final int         fps;

    /** Json read from the job description file               */
    private final JsonObject  jsonDesc;

    /** Status of the Job's instace                           */
    private JobStatus status = JobStatus.STOPPED;

    /**
     * @constructor
     *
     * @param[in]   id   Job identification
     * @param[in]   in   Directory with the image sequence to encode
     * @param[in]   fps  Image sequence frame rate (fps)
     * @param[in]   desc JSON object describing the job
     */
    public Job(final int id, final File in, final int fps,
            final JsonObject jsonDesc) {
        this.id       = id;
        this.in       = in;
        this.fps      = fps;
        this.jsonDesc = jsonDesc;
    }

    public int getId() {
        return id;
    }

    public File getIn() {
        return in;
    }

    public int getFPS() {
        return fps;
    }

    public JsonObject getJSONDescription() {
        return jsonDesc;
    }

    public JobStatus getStatus() {
        return status;
    }

    protected void setStatus(JobStatus status) {
        this.status = status;
    }
}