package hp.pipeman;

/**
 * The PipeStep interface represents a step within a Job processing
 * pipeline.
 * It is intended to build pipelines as a chain. Example:
 * 
 *  (new job) -> StepA -> StepB -> StepC
 * 
 * The processing of the new job is initiated by queuing it in the StepA. After
 * each steps do its processing on the Job, queues it into the next step.
 */
public interface PipeStep {

    /**
     * @brief Set the following step in the pipeline.
     * 
     * It is intended to be used to build the pipeline in the following way:
     *   `pipestepA.pipeStepSetNext(pipestepB).pipeStepSetNext(C);`
     * 
     * @return nextPipeStep
     */
    public PipeStep pipeStepSetNext(final PipeStep nextPipeStep);

    public void pipeStart();

    public void pipeStop();

    /**
     * @brief Queues a job to be processed by this pipeline step.
     */
    public void pipeStepQueueJob(final Job job);

}