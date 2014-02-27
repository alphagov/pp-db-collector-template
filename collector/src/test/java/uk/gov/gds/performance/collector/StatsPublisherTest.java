package uk.gov.gds.performance.collector;


import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class StatsPublisherTest {

    private final WebTarget webTarget = mock(WebTarget.class);
    private final Invocation.Builder requestBuilder = mock(Invocation.Builder.class);

    @Before
    public void setup() {
        doReturn(requestBuilder).when(webTarget).request();
    }

    @Test
    public void publish_shouldPostStatsToThePerformancePlatform() throws Exception {
        StatsPublisher publisher = new StatsPublisher(webTarget, new StageResultToJsonConverter());

        Invocation invocation = mock(Invocation.class);
        when(requestBuilder.buildPost(isA(Entity.class))).thenReturn(invocation);
        List<StageResult> results = new ArrayList<>();
        results.add(new StageResult(new LocalDate(), Period.day, "DIGITAL", 15));

        publisher.publish(results);

        verify(invocation).invoke(String.class);
    }

    @Test
    public void publish_shouldDelegateToTheConverter() throws Exception {
        Invocation invocation = mock(Invocation.class);
        ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
        when(requestBuilder.buildPost(isA(Entity.class))).thenReturn(invocation);
        StageResult expectedResult = new StageResult(new LocalDate(), Period.day, "DIGITAL", 15);
        List<StageResult> results = new ArrayList<>();
        results.add(expectedResult);
        StageResultToJsonConverter mockConverter = mock(StageResultToJsonConverter.class);
        when(mockConverter.convert(expectedResult)).thenReturn(Json.createObjectBuilder().add("foo", "bar").build());
        StatsPublisher publisher = new StatsPublisher(webTarget, mockConverter);

        publisher.publish(results);

        verify(mockConverter).convert(expectedResult);
        verify(requestBuilder).buildPost(captor.capture());
        JsonObject jsonObject = (JsonObject) captor.getValue().getEntity();
        StringWriter writer = new StringWriter();
        Json.createWriter(writer).writeObject(jsonObject);
        String body = writer.toString();
        assertEquals("{\"foo\":\"bar\"}", body);
    }
}