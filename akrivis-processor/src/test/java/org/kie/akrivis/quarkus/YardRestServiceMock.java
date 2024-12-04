package org.kie.akrivis.quarkus;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.Map;

@Path("/")
@RegisterRestClient
public class YardRestServiceMock {

    @Inject
    YardServiceMock yardServiceMock;

    @POST
    @Path("/yard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String yardResponse(Map<String, Object> payload) {
        yardServiceMock.setPayload(payload);
        return yardServiceMock.getResult();
    }
}
