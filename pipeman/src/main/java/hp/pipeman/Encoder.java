 package hp.pipeman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * The Encoder class maintains a single Job queue for all the Encoder's
 * instances. The PipeMan creates one Encoder instance per each Docker
 * Container Encoder instance that is launched.
 * <p>
 * Each Encoder instance has its own thread. Whenever a Job is queued into the
 * Encoder's queue, the first thread that is scheduled for execution, retires
 * the Job from the queue and triggers the encoding process in its associated
 * container. The thread remains blocked waiting for the encoding process to
 * complete, then it queues the Job into the next PipeStep in the pipeline 
 * chain and peeks the next Job to be processed or blocks until being signaled
 * of a new Job to be processed. 
 * @see Job
 * @see PipeStep
 * @see PipeMan
 */
class Encoder extends Thread implements PipeStep {
    /** List of all encoder instances */
    private static List<Encoder> instances = new ArrayList<Encoder>();

    /** Queue of pending jobs */
    private static final Queue<Job> jobs_queue = new ConcurrentLinkedDeque<Job>();

    /** Next step in the pipeline */
    private static PipeStep nextPipeStep = null;

    /** Id of the encoder's instance - this is the doker container id */
    private final String encoderId;

    /**
     * Encoder constructor
     * @param encoderId  the docker container id to be associated with this 
     *                   encoder instance.
     */
    public Encoder(final String encoderId) {
        super();
        this.encoderId = encoderId;
        instances.add(this);
    }

    /**
     * Set the next step in the pipeline for all the encoder instances.
     * Calling this function in any Encoder instance will set the next step for
     * all the instances.
     * @param nextPipeStep   the next step in the pipeline for all the encoders
     * 
     * @return the same nextPipeStep passed as argument. 
     */
    @Override
    public final PipeStep pipeStepSetNext(final PipeStep nextPipeStep) {
        // This is global to all encoders
        Encoder.nextPipeStep = nextPipeStep;
        return nextPipeStep;
    }

    /**
     * Recursively start the pipeline steps' threads from back to front.
     * Only one encoder instance will receive this invocation. It is responsible
     * for starting the threads of all the remaining Encoder's instances.
     */
    @Override
    public void pipeStart() {
        // Recursively, start the pipeline steps' threads from back to front.
        if (nextPipeStep != null) {
            nextPipeStep.pipeStart();
        }
        // This is global to all encoders, so start all encoders' threads
        for (Encoder e: instances) {
            e.start(); 
        }
    }

    /**
     * Recursively stop the pipeline steps' threads from front to back.
     * Only one encoder instance will receive this invocation. It is responsible
     * for stopping the threads of all the remaining Encoder's instances.
     */
    @Override
    public void pipeStop() {
        // Recursively, stop the pipeline steps' threads from front to back.
        try {
            // This is global to all encoders, so stop all encoders' threads
            for (Encoder e: instances) {
                e.interrupt(); // Interrupt this pipeline step's thread 
            }

            for (Encoder e: instances) {
                e.join();      // and wait it to terminate
            }
        } catch (InterruptedException ie) {
            // do nothing
        }

        if (nextPipeStep != null) {
            nextPipeStep.pipeStop();
        }
    }

    /**
     * Queue the given Job into the Encoder's common Job queue.
     * Any Encoder instance may receive this call.
     * 
     * @param job   the Job to be queued and processed by this step
     */
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
     * interrupted.
     * 
     * All encoder instances look for Jobs in the same jobs_queue. The first to
     * poll the job from the queue, processes it. 
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

    /**
     * Thread's run method.
     */
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
        System.out.println("[ENC-" + encoderId + "]:" + msg);
    }

    protected boolean doProcessJob(Job job) {
        // Launch an encoder docker container.
        // TODO: improve this by using a docker API library such as 
        // Spotify's docker-client.
        // Ref.: https://github.com/spotify/docker-client/
        // Alternatively, implement some communication mechanism between the
        // Pipeline Manager and the Encoder, in order to avoid using docker for
        // start the decoding and retrieve progress status.
        int ret = 0;
        try {
            final String docker_cmd[] = {
                    "docker", "exec",
                    "-i", encoderId,
                    "/encode.sh", job.getIn().getName(), "" + job.getFPS()
            };

            pipeStepLog("Invoking docker: '" + Arrays.toString(docker_cmd) +
                    "'");

            ProcessBuilder prb = new ProcessBuilder();
            prb.command(docker_cmd);
            prb.redirectErrorStream(true);
            final Process pr = prb.start();
            
            // Lets get the output from the docker container
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(pr.getInputStream()));
            ret = pr.waitFor();
            String line;
            while ((line = reader.readLine()) != null) {
                pipeStepLog("[DOCKED]:" + line);
            }

        } catch (InterruptedException ie) {
            ie.printStackTrace(System.out);
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace(System.out);
            return false;
        }

        return ret == 0;
    }
}