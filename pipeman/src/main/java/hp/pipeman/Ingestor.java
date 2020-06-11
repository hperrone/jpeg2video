package hp.pipeman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * The Ingestor is the PipeStep responsible for monitoring the Ingest Storage
 * for new image sequences to be processed, and creating the Job instances for
 * them. The Ingestor fills the Job by loading the information contained in the
 * image sequence's job description file.
 * <p>
 * Since images sequences may require some time to be entirely copied/moved
 * into the Ingest Storage, the Ingestor only start a Job after confirming that
 * the number of files in the directory is equal or greater than the number of
 * frames specified in the job description file.
 * <p>
 * Most of the PipeStep functionality is provided by BaseThreadPipeStep.
 */
public class Ingestor extends BaseThreadPipeStep {

    /** full path to the directory to look after new image sequeces */
    private File pathToMonitor; 

    /** internal job counter, currently only useful for logging */
    private static int jobCounter = 1;
    
    public Ingestor(File pathToMonitor) {
        super("INGEST");
        this.pathToMonitor = pathToMonitor;
    }

    /**
     * Since the ingestor is the first step in the pipeline, it must create the
     * job to be processed.
     */
    @Override
    protected Job getNextJob() {
        Job job = null;
        try {
            while (job == null) {
                File files[] = pathToMonitor.listFiles();
                for(File f : files) {
                    job = checkPathForJob(f);
                    if (job != null) {
                        break;
                    }
                }

                if (job == null) {
                    Thread.sleep(1000);
                }
            }
        } catch (InterruptedException ie) {
        } catch (RuntimeException rt) {
            rt.printStackTrace(System.out);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return job;
    }

    @Override
    protected boolean doProcessJob(Job job) {
        // No processing is needed for the ingestor
        return true;
    }

    private Job checkPathForJob(final File path) {
        Job job = null;
        InputStream is = null;
        try {
            // Jobs must be in directories
            if (!path.isDirectory()) {
                return null;
            }

            // Jobs must have a job description file
            File fdesc = new File(path.getCanonicalPath() + "/jobdesc.json");
            if (!fdesc.exists()) {
                return null;
            }

            // Read the contents of the job description file
            is = new FileInputStream(fdesc);
            JsonObject jsonDesc = Json.createReader(is).readObject();
            int frames_n = jsonDesc.getInt("frames_n");
            int fps = jsonDesc.getInt("fps");
            is.close();

            // Since copying all the frame files to the ingest directory is not
            // an atomic operation, before starting the job, confirm that all
            // frames files are already there. (simply count the images file in
            // the directory and match frames_n from the job description file). 
            int files_n = path.listFiles(new FilenameFilter(){
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jpg");
                }
            }).length;

            if (files_n >= frames_n) {
                // It is a job ready to be processed!
                job = new Job(jobCounter, path, fps, jsonDesc);
                jobCounter++;

                // Delete the job descriptor file, so it is not processed again.
                Files.deleteIfExists(fdesc.toPath());    
            }

        } catch(IOException ioe) {
            ioe.printStackTrace(System.out);
        } finally {
            if (is != null) {
                try { is.close(); } catch(IOException ioe) {}
            }
        }
        return job;
    }
}