package org.kie.akrivis.quarkus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.akrivis.dbmodel.result.Configuration;
import org.akrivis.dbmodel.result.Output;
import org.kie.yard.api.model.Input;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InputLoader {

    private final Payloads payloads;
    private HashMap<String, Object> loaders = new HashMap<>();

    public InputLoader(Payloads payloads) {
        this.payloads = payloads;
    }

    public Map<String, Object> resolve(final Configuration configuration,
                                       final List<Input> inputs) throws NotFoundException {

        final Map<String, Object> loaders = getLoaders(configuration, inputs);

        final HashMap<String, Object> result = new HashMap<>();
        for (final String key : loaders.keySet()) {
            result.put(key, loaders.get(key));
        }
        return result;
    }

    private Map<String, Object> getLoaders(final Configuration configuration,
                                           final List<Input> inputs) throws NotFoundException {

        for (Input input : inputs) {
            final Object o = find(input, configuration);
            loaders.put(input.getName(), o);
        }

        return loaders;
    }

    private Object find(final Input input,
                        final Configuration configuration) throws NotFoundException {

        for (Output output : configuration.outputs) {
            if (Objects.equals(input.getName(), output.name)) {

                if (output.from.startsWith("akrivis")) {
                    return get(output.from);
                } else {
                    try {
                        return new ObjectMapper()
                                .readValue(get(configuration.api), Map.class)
                                .get(output.from);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        throw new NotFoundException(input.getName());
    }

    private String get(final String api) throws NotFoundException {
        for (Payload payload : payloads.getPayloads()) {
            if (Objects.equals(payload.getOrigin(), api)) {
                return payload.getData();
            }
        }
        throw new NotFoundException(api);
    }
}
