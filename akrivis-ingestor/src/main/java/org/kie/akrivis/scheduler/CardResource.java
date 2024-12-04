package org.kie.akrivis.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.akrivis.dbmodel.*;
import org.akrivis.dbmodel.result.Configuration;
import org.akrivis.dbmodel.result.Threshold;
import org.kie.akrivis.scheduler.responses.CardData;
import org.kie.akrivis.scheduler.responses.Result;
import org.kie.akrivis.utility.BackstageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Path("/card")
public class CardResource {

    private static final Logger LOG = Logger.getLogger(CardResource.class.getName());

    @Inject
    CardRepository cardRepository;

    @Inject
    ResultRepository resultRepository;

    private static final String DEFINITION = "definition";

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public void add(Map<String, Object> payload) {
        try {

            final CardConfiguration cardConfiguration = getConfiguration(payload.get("configuration"));
            final Card card = getCard(payload.get("card"));
            cardRepository.getEntityManager().persist(cardConfiguration);
            card.configuration = cardConfiguration;
            cardRepository.persist(card);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response delete(@PathParam("id") Long cardId) {
        cardRepository.deleteCard(cardId);
        LOG.info("Card Deleted: " + cardId);
        return Response.noContent().build();
    }

    @GET
    @Path("/list")
    public BackstageResponse<List<Card>> listCards() {
        return new BackstageResponse<>(cardRepository.findAll().stream().toList());
    }

    @GET
    @Path("{id}/data")
    public CardData cardData(@PathParam("id") Long cardId) throws JsonProcessingException {
        final Card card = cardRepository.findById(cardId);

        final CardData cardData = new CardData() {{
            cardDefinition = jsonToYaml(card.definition);
            configurationDefinition = jsonToYaml(card.configuration.definition);
        }};

        return cardData;
    }

    @GET
    @Path("{id}/history")
    public BackstageResponse<List<Result>> history(@PathParam("id") Long cardId) {
        final List<Result> list = resultRepository.history(cardId).stream()
                .map(runResult -> {
                    try {
                        return formResult(runResult);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).toList();

        return new BackstageResponse<>(list);
    }

    @GET
    @Path("/results")
    public BackstageResponse<List<Result>> results() {
        final List<Result> results = new ArrayList<>();
        final List<Card> cards = cardRepository.findAll().stream().toList();

        cards.forEach(card -> {
                    resultRepository.latest(card.id).map(runResult ->
                    {
                        try {
                            return results.add(formResult(runResult));
                        } catch (JsonProcessingException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                }
        );
        return new BackstageResponse<>(results);
    }

    private Result formResult(final RunResult runResult) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();

        return new Result(
                runResult.card.id,
                runResult.runTime.getEpochSecond(),
                runResult.status,
                objectMapper.readValue(runResult.measureValue, Map.class),
                runResult.measureName,
                runResult.maxValue,
                jsonToYaml(runResult.cardData),
                getThresholds(runResult)
            );
    }

    private static String jsonToYaml(String json) throws JsonProcessingException {
        final JsonNode jsonNode = new ObjectMapper().readTree(json);

        return new YAMLMapper()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .writeValueAsString(jsonNode);
    }

    private static Threshold[] getThresholds(RunResult runResult) {

        try {
            final ObjectMapper jsonMapper = new ObjectMapper();
            final Configuration configuration = jsonMapper.readValue(runResult.configurationData, Configuration.class);
            return configuration.thresholds;
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // TODO report/log
        }

        return new Threshold[0];
    }

    private CardConfiguration getConfiguration(Object o) throws JsonProcessingException {
        if (o instanceof Map map) {
            final CardConfiguration configuration = new CardConfiguration();
            if (map.get(DEFINITION) instanceof String yaml) {
                final Map yamlMap = new ObjectMapper(new YAMLFactory()).readValue(yaml, Map.class);
                configuration.definition = new ObjectMapper().writeValueAsString(yamlMap);
            }
            return configuration;
        }
        throw new IllegalArgumentException("Card configuration data was not in correct format.");
    }

    private Card getCard(Object o) throws JsonProcessingException {
        if (o instanceof Map map) {
            final Card card = new Card();
            if (map.get(DEFINITION) instanceof String yaml) {
                final Map yamlMap = new ObjectMapper(new YAMLFactory()).readValue(yaml, Map.class);
                card.definition = new ObjectMapper().writeValueAsString(yamlMap);
            }

            return card;
        }
        throw new IllegalArgumentException("Card data was not in correct format.");
    }
}
