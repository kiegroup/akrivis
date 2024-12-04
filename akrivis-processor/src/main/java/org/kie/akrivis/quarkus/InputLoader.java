package org.kie.akrivis.quarkus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.akrivis.dbmodel.result.Configuration;
import org.akrivis.dbmodel.result.Output;
import org.kie.yard.api.model.Input;

import java.util.*;

public class InputLoader {

    private final Payloads payloads;

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
        final HashMap<String, Object> loaders = new HashMap<>();
        final List<Input> notFound = new ArrayList<>();

        for (Input input : inputs) {
            final Optional<Object> o = find(input, configuration);
            if (o.isPresent()) {
                loaders.put(input.getName(), o.get());
            } else {
                notFound.add(input);
            }
        }

        if (!notFound.isEmpty()) {
            throw new NotFoundException(notFound);
        }

        return loaders;
    }

    private Optional<Object> find(final Input input,
                                  final Configuration configuration) {

        for (Output output : configuration.outputs) {
            if (Objects.equals(input.getName(), output.name)) {

                if (output.from.startsWith("akrivis")) {
                    final Optional<String> s = get(output.from);
                    if (s.isPresent()) {
                        return Optional.of(s);
                    } else {
                        return Optional.empty();
                    }
                } else {
                    final Optional<String> s = get(configuration.api);

                    if (s.isPresent()) {
                        try {
                            final Map map = new ObjectMapper().readValue(s.get(), Map.class);
                            return Optional.of(map.get(output.from));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return Optional.empty();
                    }
                }
            }
        }

        return Optional.empty();
    }

    public Optional<String> get(String api) {
        for (Payload payload : payloads.getPayloads()) {
            if (Objects.equals(payload.getOrigin(), api)) {
                return Optional.of(payload.getData());
            }
        }
        return Optional.empty();
    }
}
