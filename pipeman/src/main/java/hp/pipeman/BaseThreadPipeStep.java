package hp.pipeman;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class BaseThreadPipeStep extends Thread
        implements PipeStep {

    /** Next step in the pipeline */
    private PipeStep nextPipeStep = null;

    /** Name of the pipeline step. Used for logging. */
    private final String pipeStepName;

    /** Queue of pending jobs */
    private final Queue<Job> jobs_queue = new ConcurrentLinkedDeque<Job>();

    protected BaseThreadPipeStep(String name) {
        super();
        this.pipeStepName = name;
    }

    @Override
    public final PipeStep pipeStepSetNext(final PipeStep nextPipeStep) {
        this.nextPipeStep = nextPipeStep;
        return nextPipeStep;
    }

    @Override
    public void pipeStart() {
        // Recursively, start the pipeline steps' threads from back to front.
        if (nextPipeStep != null) {
            nextPipeStep.pipeStart();
        }

        start(); // Start this pipeline step's thread
    }

    @Override
    public void pipeStop() {
        // Recursively, stop the pipeline steps' threads from front to back.
        try {
            interrupt(); // Interrupt this pipeline step's thread 
            join();      // and wait it to terminate
        } catch (InterruptedException ie) {
            // do nothing
        }

        if (nextPipeStep != null) {
            nextPipeStep.pipeStop();
        }
    }

    @Override
    public void pipeStepQueueJob(final Job job) {
        synchronized(jobs_queue) {
            pipeStepLog("Queuing JOB #" + job.getId());
            jobs_queue.add(job);
            jobs_queue.notifyAll();
        }
    }

    /**
     * @brief Wait for the next job to be processed
     *
     * The function blocks until a new job is available to be processed.
     * @return the next job to process or null if the thread have been
     * interrupted
     */
    protected Job getNextJob() {
        Job job = null;

        synchronized(jobs_queue) {
            // Wait for a Job
            while((job = jobs_queue.poll()) == null) {
                pipeStepLog("Waiting for a job");
                try {
                    jobs_queue.wait(5000);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        return job;
    }

    @Override
    public void run() {
        pipeStepLog("Started");
        while(!Thread.currentThread().isInterrupted()) {
            Job curr_job = getNextJob();
            if (curr_job == null) {
                // The thread has been interrupted
                continue; 
            }

            // At this point, the worker has a job. Lets start working.
            pipeStepLog("Processing JOB #" + curr_job.getId());

            synchronized(curr_job) {
                curr_job.setStatus(Job.JobStatus.RUNNING);
            }

            boolean ret = false;
            try {
                ret = doProcessJob(curr_job);
            } catch (RuntimeException re) {
                // Some basic error handling so the pipeline is not broken due
                // to any problem with a particular job or step.
                ret = false;
                re.printStackTrace(System.out);
            }

            synchronized(curr_job) {
                if (!ret) {
                    curr_job.setStatus(Job.JobStatus.ERROR);
                } else if (nextPipeStep == null) {
                    curr_job.setStatus(Job.JobStatus.COMPLETED);
                }
            }

            pipeStepLog("Processed JOB #" + curr_job.getId());

            // At this point, this step is completed job. Lets invoke the next.
            if (nextPipeStep != null) {
                try {
                    nextPipeStep.pipeStepQueueJob(curr_job);
                } catch (RuntimeException re) {
                    // Some basic error handling so the pipeline is not broken
                    // due to any problem with a particular job or step.
                    re.printStackTrace(System.out);
                    synchronized(curr_job) {
                        curr_job.setStatus(Job.JobStatus.ERROR);
                    }
                }
            }
        }

        pipeStepLog("Ended");
    }

    /**
     * @brief Logs message to standard output prefixing the pipe step name
     * 
     * @param[in]  msg    log message
     */
    protected void pipeStepLog(final String msg) {
        System.out.println("[" + pipeStepName + "]:" + msg);
    }

    /**
     * @brief Implements the processing of the job
     *
     * @param[in,out] job    Job to be processed by this pipeline step
     *
     * @retval true    on success
     * @retval false   on error processing the job
     */
    protected abstract boolean doProcessJob(final Job job);
}