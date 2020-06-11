package hp.pipeman;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

class Publisher extends BaseThreadPipeStep {
    private static Publisher sm_instance = null;
 
    public static Publisher getInstance() {
        synchronized (Publisher.class) {
            if (sm_instance == null) {
                sm_instance = new Publisher();
            }
        }

        return sm_instance;
    }

    /** Singleton private constructor */
    private Publisher() {
        super("PUBLISH");
    }
    
    @Override
    protected boolean doProcessJob(final Job job) {
        JsonArray jsonFeeds;

        try {
            InputStream is = new FileInputStream("/jobs_out/streams.json");
            jsonFeeds = Json.createReader(is).readArray();
            is.close();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            jsonFeeds = Json.createReader(new StringReader("[]")).readArray();
        }
        
        JsonObject jobDesc = job.getJSONDescription();
        jobDesc = insertValue(jobDesc, "dir", job.getIn().getName());
        jsonFeeds = insertValue(jsonFeeds, jobDesc);
        
        try {
            OutputStream os = new FileOutputStream("/jobs_out/streams.json");
            JsonWriter writer = Json.createWriter(os);
            writer.writeArray(jsonFeeds);
            os.close();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }

        return true;
    }

    private JsonObject insertValue(JsonObject src, String key, String val) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, val);
        src.entrySet().forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    private JsonArray insertValue(JsonArray src, JsonObject val) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        src.forEach(e -> builder.add(e));
        builder.add(val);
        return builder.build();
    }
}