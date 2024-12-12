package org.kie.akrivis.quarkus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import org.akrivis.dbmodel.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessorTestBase {


    @Inject
    Processor processor;

    @Inject
    JobRepository jobRepository;

    @Inject
    CardRepository cardRepository;

    @Inject
    ResultRepository resultRepository;

    @Inject
    YardServiceMock yardServiceMock;


    public void before(final String endpoint, String sourceFolder) throws IOException {
        before(endpoint, sourceFolder, getRawData(sourceFolder));
    }

    public void before(final String endpoint, String sourceFolder, Map<Instant, String> rawDataJSONs) throws IOException {

        // Make job and raw data for it
        final Job job = new Job();
        job.endpoint = endpoint;
        job.status = JobStatus.SCHEDULED;
        job.cron = "*/5 * * * * ?";
        job.lastRun = Instant.now();
        job.type = "Default";
        jobRepository.persist(job);


        for (Map.Entry<Instant, String> rawDataJSON : rawDataJSONs.entrySet()) {
            final RawData rawData = new RawData();
            rawData.createdAt = rawDataJSON.getKey();
            rawData.job = job;
            rawData.data = rawDataJSON.getValue();
            jobRepository.getEntityManager().persist(rawData);
        }

        // Make card
        final String conf = read(Path.of("src/test/resources/" + sourceFolder + "/configuration.yml"));
        final String code = read(Path.of("src/test/resources/" + sourceFolder + "/yard.yml"));

        final Card card = new Card();
        card.definition = code;
        final CardConfiguration configuration = new CardConfiguration();
        configuration.definition = conf;
        card.configuration = configuration;

        cardRepository.getEntityManager().persist(configuration);
        cardRepository.persist(card);
    }

    public void assertPayloadContentIsValid(final Map<String, Object> payload) {
        assertTrue(payload.containsKey("yard"));
        assertTrue(payload.containsKey("input"));
        assertEquals(2, payload.keySet().size());
        assertYardContainsKeys(payload, "specVersion", "kind", "name", "inputs", "elements");
    }

    public void assertYardContainsKeys(final Map<String, Object> payload,
                                       final String... keys) {
        for (String key : keys) {
            assertTrue(((Map) payload.get("yard")).containsKey(key));
        }
    }

    public void assertInputContainsKeys(final Map<String, Object> payload,
                                        final String... keys) {
        for (String key : keys) {
            assertTrue(((Map) payload.get("input")).containsKey(key));
        }
    }

    private static String read(Path path) throws IOException {
        final String yaml = Files.readString(path);
        final Map yamlMap = new ObjectMapper(new YAMLFactory()).readValue(yaml, Map.class);
        return new ObjectMapper().writeValueAsString(yamlMap);
    }


    public static Map<Instant, String> getRawData(String sourceFolder) throws IOException {
        final Map<Instant, String> result = new HashMap<>();

        for (Path path : Files.newDirectoryStream(Path.of("src/test/resources/" + sourceFolder))) {
            final String string = path.getFileName().toString();
            if (string.contains("rawdata")) {
                final int rawdataEnd = string.indexOf("rawdata") + "rawdata".length();
                final String substring = string.substring(rawdataEnd, string.indexOf(".", rawdataEnd));

                result.put(getTime(substring), read(path));
            }
        }

        return result;
    }


    public long assertOneCardNoResultHistory() {
        final List<Card> list = cardRepository.findAll().stream().toList();
        assertEquals(1, list.size());

        final long cardId = list.get(0).id;

        assertTrue(resultRepository.history(cardId).isEmpty());
        assertFalse(resultRepository.latest(cardId).isPresent());
        return cardId;
    }

    private static Instant getTime(String substring) {
        if (!substring.isEmpty()) {
            LocalDate localDate = LocalDate.parse(substring.substring(1), DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
            return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        }
        return Instant.now();
    }

}
