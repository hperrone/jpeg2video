package hp.pipeman;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PipeMan {

    private static List<String> getEncodersId() {
        List<String> encoderIds = new ArrayList<String>();

        File encFile = new File("/encoders");
        File files[];
        while ((files = encFile.listFiles()).length == 0) {
            try { 
                Thread.sleep(1000);
            } catch (InterruptedException ie) { 
                break;
            }
        }

        for (File f: files) {
            encoderIds.add(f.getName());
        }

        return encoderIds;
    }

    public static void main(String args[]) {
        File inPath = new File("/jobs_in");
        //File outPath = new File("/jobs_out");

        System.out.println("[PIPELINE]: Settign up");

        Ingestor ingestor = new Ingestor(inPath);
        List<String> encoderIds = getEncodersId();
        Encoder encoder = null;
        for(String id : encoderIds) {
            encoder = new Encoder(id);
        }
        
        Publisher publisher = Publisher.getInstance();
        ingestor.pipeStepSetNext(encoder).pipeStepSetNext(publisher);
        ingestor.pipeStart();

        try {
            publisher.join();
        } catch (InterruptedException ie) {

        }
        
    }

}