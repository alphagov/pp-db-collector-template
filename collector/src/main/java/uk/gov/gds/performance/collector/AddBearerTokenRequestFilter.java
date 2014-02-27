package uk.gov.gds.performance.collector;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

public class AddBearerTokenRequestFilter implements ClientRequestFilter {
    private final String headerValue;

    public AddBearerTokenRequestFilter(String bearerToken) {
        this.headerValue = "Bearer " + bearerToken;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add("Authorization", headerValue);
    }
}
