package uk.gov.gds.performance.collector;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.util.List;

public class StatsPublisher {
    private final WebTarget target;
    private StageResultToJsonConverter converter;

    public StatsPublisher(WebTarget target, StageResultToJsonConverter converter) {
        this.target = target;
        this.converter = converter;
    }

    public void publish(List<StageResult> results) throws IOException {
        for (StageResult result : results) {
            target.request().buildPost(Entity.json(converter.convert(result))).invoke(String.class);
        }
    }
}
