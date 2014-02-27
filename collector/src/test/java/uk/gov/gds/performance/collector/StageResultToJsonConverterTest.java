package uk.gov.gds.performance.collector;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import uk.co.o2.json.schema.ErrorMessage;
import uk.co.o2.json.schema.JsonSchema;
import uk.co.o2.json.schema.SchemaPassThroughCache;
import uk.co.o2.json.schema.jaxrs.ClasspathSchemaLookup;
import uk.co.o2.json.schema.jaxrs.SchemaLookup;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StageResultToJsonConverterTest {

    @Test
    public void convert_shouldCreateAJsonObjectWithTheCorrectValues() throws Exception {
        StageResultToJsonConverter convertor = new StageResultToJsonConverter();
        LocalDate now = LocalDate.now();
        StageResult result = new StageResult(now, Period.week, "DIGITAL", 50);

        JsonObject json = convertor.convert(result);

        assertEquals(5, json.size());
        assertEquals("week", json.getString("_period"));
        assertEquals(now.toDateTimeAtStartOfDay().toString(ISODateTimeFormat.dateTimeNoMillis()), json.getString("_timestamp"));
        assertEquals("digital", json.getString("channel"));
        assertEquals(50, json.getInt("count"));
        assertNotNull(json.get("_id"));
    }

    @Test
    public void publish_shouldConvertStageResultsIntoSchemaCompliantJson() throws IOException {
        SchemaLookup schemaLookup = new ClasspathSchemaLookup();
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory(mapper);
        SchemaPassThroughCache cache = new SchemaPassThroughCache(jsonFactory);
        JsonSchema schema = cache.getSchema(schemaLookup.getSchemaURL("slc_data_schema.json"));
        StageResultToJsonConverter convertor = new StageResultToJsonConverter();
        StageResult result = new StageResult(new LocalDate(), Period.day, "DIGITAL", 15);

        JsonObject jsonResult = convertor.convert(result);

        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(writer);
        jsonWriter.writeObject(jsonResult);
        jsonWriter.close();

        List<ErrorMessage> errorMessages = schema.validate(mapper.readTree(writer.toString()));
        assertEquals(Collections.<ErrorMessage>emptyList(), errorMessages);
    }
}
