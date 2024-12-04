package org.kie.akrivis.quarkus;

public class AkrivisPayload extends Payload {

    public final static String CREATED_AT = "akrivis.createdAt";
    public final static String DATA = "akrivis.data";

    public AkrivisPayload(String origin, String data) {
        super(origin, data);
    }
}
