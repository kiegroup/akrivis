package org.akrivis.dbmodel.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Configuration {
    public String name;
    public String api;

    public Number sampling;

    public Output[] outputs;

    @JsonProperty("max value")
    public Number maxValue = 100;

    public Threshold[] thresholds;

    public Configuration() {

    }

    public Configuration(String name, String api, Number sampling, Number maxValue, Output[] outputs, Threshold[] thresholds) {
        this.name = name;
        this.api = api;
        this.sampling = sampling;
        this.maxValue = maxValue;
        this.outputs = outputs;
        this.thresholds = thresholds;
    }
}
