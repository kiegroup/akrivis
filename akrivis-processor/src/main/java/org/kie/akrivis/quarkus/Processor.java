package org.kie.akrivis.quarkus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import org.akrivis.dbmodel.*;
import org.akrivis.dbmodel.result.Configuration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.yard.api.model.YaRD;
import org.kie.yard.api.model.YaRD_JsonMapperImpl;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class Processor {

    @Inject
    JobRepository jobRepository;

    @Inject
    CardRepository cardRepository;

    @Inject
    ResultRepository resultRepository;

    @ConfigProperty(name = "akrivis.evaluator.address")
    String service;

    @Transactional
    @Scheduled(every = "10s")
    public void scores() {

        try (Client client = ClientBuilder.newClient()) {
            final String s = service + "/yard";
            for (Request request : getRequests()) {
                try {
                    final Map mapRecord = client.target(s).request(MediaType.APPLICATION_JSON).post(Entity.entity(request.json, MediaType.APPLICATION_JSON), Map.class);

                    final RunResult runResult = new RunResult();
                    runResult.measureName = request.yardModel.getName();
                    runResult.status = "Figure this out";
                    runResult.measureValue = getFormatted(mapRecord.get("Result"));
                    runResult.maxValue = 100; // TODo 100 will do for now
                    runResult.cardData = request.card.definition;
                    runResult.configurationData = request.card.configuration.definition;
                    runResult.card = request.card;
                    runResult.runTime = Instant.now();

                    cardRepository.getEntityManager().persist(runResult);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String getFormatted(final Object o) throws JsonProcessingException {
        if (o instanceof Integer result) {
            return "{ \"score\": %d }".formatted(result);
        } else {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        }
    }

    private List<Request> getRequests() {
        final List<Request> result = new ArrayList<>();

        cardRepository.findAll().stream().forEach(card -> {
            try {
                final Map<String, Object> maps = new HashMap();
                final ObjectMapper objectMapper = new ObjectMapper();
                final Configuration configuration = objectMapper.readValue(card.configuration.definition, Configuration.class);

                final Optional<RawData> rawData = jobRepository.findLatestRawDataByEndPoint(configuration.api);

                if (rawData.isPresent()) {
                    final Optional<RunResult> latestResult = resultRepository.latest(card.id);

                    if (!latestResult.isPresent() || latestResult.get().runTime.isBefore(rawData.get().createdAt)) {

                        final YaRD model = new YaRD_JsonMapperImpl().fromJSON(card.definition);
                        final Map<String, Object> yardInputs = new InputDataProducerForYard(jobRepository).formInputData(rawData.get(), configuration, model);

                        maps.put("yard", objectMapper.readValue(card.definition, Map.class));
                        maps.put("input", yardInputs);

                        result.add(new Request(card, model, maps));
                    }
                }
            } catch (NotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        });
        return result;
    }

    private record Request(Card card, YaRD yardModel, Map<String, Object> json) {
    }
}
