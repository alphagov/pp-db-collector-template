package uk.gov.gds.performance.collector;

import org.junit.Test;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import static org.mockito.Mockito.*;

public class AddBearerTokenRequestFilterTest {
    private final String bearerToken = "TheBearerToken";
    private final ClientRequestFilter filter = new AddBearerTokenRequestFilter(bearerToken);
    private final ClientRequestContext requestContext = mock(ClientRequestContext.class);

    @Test
    public void filter_shouldAddAnAuthorizationHeaderWithABearerToken() throws Exception {

        @SuppressWarnings("unchecked")
        MultivaluedMap<String,Object> mockHeaders = (MultivaluedMap<String,Object>) mock(MultivaluedMap.class);
        when(requestContext.getHeaders()).thenReturn(mockHeaders);

        filter.filter(requestContext);

        verify(mockHeaders).add("Authorization", "Bearer " + bearerToken);
    }
}