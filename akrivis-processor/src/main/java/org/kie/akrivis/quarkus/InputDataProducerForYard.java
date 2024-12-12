package org.kie.akrivis.quarkus;

import org.akrivis.dbmodel.JobRepository;
import org.akrivis.dbmodel.RawData;
import org.akrivis.dbmodel.result.Configuration;
import org.akrivis.dbmodel.result.Output;
import org.kie.yard.api.model.YaRD;

import java.util.*;

public class InputDataProducerForYard {

    private static final String STREAM = "stream";

    private JobRepository jobRepository;

    public InputDataProducerForYard(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Map<String, Object> formInputData(final RawData rawData,
                                             final Configuration configuration,
                                             final YaRD model) throws NotFoundException {

        final Optional<Integer> sampling = getSampling(configuration);

        if (sampling.isEmpty()) {
            return getInput(rawData, configuration, model);
        } else {

            boolean addSpotValues = true;
            final List<String> streams = getStreams(configuration);
            final Map<String, Object> result = new HashMap<>();
            final long jobId = rawData.job.id;

            for (RawData data : jobRepository.findRawDataByJobId(jobId, sampling.get())) {
                for (Map.Entry<String, Object> entry : getInput(data, configuration, model).entrySet()) {

                    if (streams.contains(entry.getKey())) {
                        if (!result.containsKey(entry.getKey())) {
                            result.put(entry.getKey(), new ArrayList<>());
                        }
                        if (result.get(entry.getKey()) instanceof List list) {
                            list.add(entry.getValue());
                        }
                    } else if (addSpotValues) {
                        result.put(entry.getKey(), entry.getValue());
                    }

                    addSpotValues = false;
                }
            }

            return result;
        }
    }

    private Optional<Integer> getSampling(final Configuration configuration) {

        if (configuration.sampling != null) {
            return Optional.of((int) configuration.sampling);
        }

        return Optional.empty();
    }

    private List<String> getStreams(final Configuration configuration) {
        final List<String> result = new ArrayList<>();

        for (Output output : configuration.outputs) {
            if (Objects.equals(STREAM, output.type)) {
                result.add(output.name);
            }
        }

        return result;
    }

    private Map<String, Object> getInput(final RawData rawData,
                                         final Configuration configuration,
                                         final YaRD model) throws NotFoundException {
        final Payloads payloads = getPayloads(rawData);
        final InputLoader inputLoader = new InputLoader(payloads);
        return inputLoader.resolve(configuration, model.getInputs());
    }

    private Payloads getPayloads(final RawData rawData) {
        final Payloads payloads = new Payloads();
        payloads.getPayloads().add(new Payload(rawData.job.endpoint, rawData.data));
        payloads.getPayloads().add(new AkrivisPayload(AkrivisPayload.CREATED_AT, Long.toString(rawData.createdAt.toEpochMilli())));
        payloads.getPayloads().add(new AkrivisPayload(AkrivisPayload.DATA, rawData.data));
        return payloads;
    }
}
