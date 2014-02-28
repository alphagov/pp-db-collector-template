package uk.gov.gds.performance.collector;

import org.joda.time.format.ISODateTimeFormat;

import javax.json.Json;
import javax.json.JsonObject;

public class StageResultToJsonConverter {

    public JsonObject convert(StageResult stageResult) {
        return Json.createObjectBuilder()
                .add("_id", stageResult.get_id())
                .add("_timestamp", stageResult.getTimestamp().toDateTimeAtStartOfDay().toString(ISODateTimeFormat.dateTimeNoMillis()))
                .add("period", stageResult.getPeriod().toString())
                .add("channel", stageResult.getChannel().toLowerCase())
                .add("count", stageResult.getCount())
                .build();
    }

}