package org.kie.akrivis.quarkus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.akrivis.dbmodel.result.Configuration;
import org.junit.jupiter.api.Test;
import org.kie.yard.api.model.Input;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InputLoaderTest {

    private static final String TEST_DATA = """
            {
                "total": 0
            }
                """;

    @Test
    void testEmpty() throws NotFoundException {

        final Map<String, Object> resolve = new InputLoader(new Payloads())
                .resolve(new Configuration(), new ArrayList<>());

        assertTrue(resolve.isEmpty());
    }

    @Test
    void testNoPayloadsUsed() throws NotFoundException {

        final Payloads payloads = getPayloads();
        final Map<String, Object> resolve = new InputLoader(payloads)
                .resolve(new Configuration(), new ArrayList<>());

        assertTrue(resolve.isEmpty());
    }

    @Test
    public void testMapValue() throws NotFoundException, IOException {

        final Payloads payloads = getPayloads();

        final ArrayList<Input> inputs = new ArrayList<>();
        final Input input = new Input();
        input.setName("Number of Jira Issues");
        input.setType("number");
        inputs.add(input);

        final Configuration configuration = loadConf("card");
        final Map<String, Object> resolve = new InputLoader(payloads)
                .resolve(configuration, inputs);

        assertEquals(1, resolve.size());
        assertEquals(0, resolve.get("Number of Jira Issues"));
    }

    @Test
    public void testMapMetaValues() throws NotFoundException, IOException {

        final Payloads payloads = getPayloads();

        final ArrayList<Input> inputs = new ArrayList<>();
        final Input createdAtInput = new Input();
        createdAtInput.setName("Created");
        createdAtInput.setType("date");
        inputs.add(createdAtInput);
        final Input dataInput = new Input();
        dataInput.setName("Data");
        dataInput.setType("string");
        inputs.add(dataInput);

        final Configuration configuration = loadConf("card" + File.separator + "metadata");
        final Map<String, Object> resolve = new InputLoader(payloads)
                .resolve(configuration, inputs);

        assertEquals(2, resolve.size());
        assertEquals("10000", resolve.get("Created"));
        assertEquals(TEST_DATA, resolve.get("Data"));
    }

    @Test
    public void testInputNotFound() throws NotFoundException, IOException {

        final Payloads payloads = getPayloads();

        final ArrayList<Input> inputs = new ArrayList<>();
        final Input input = new Input();
        input.setName("Missing");
        input.setType("number");
        inputs.add(input);

        final Configuration configuration = loadConf("card");
        NotFoundException notFoundException = assertThrows(
                NotFoundException.class,
                () ->
                        new InputLoader(payloads)
                                .resolve(configuration, inputs));
        assertEquals("Not found Missing", notFoundException.getMessage());

    }

    private static Payloads getPayloads() {
        final Payloads payloads = new Payloads();
        payloads.getPayloads().add(new AkrivisPayload(AkrivisPayload.CREATED_AT, "10000"));
        payloads.getPayloads().add(new AkrivisPayload(AkrivisPayload.DATA, TEST_DATA));
        payloads.getPayloads().add(new Payload("http://test.com", TEST_DATA));
        return payloads;
    }

    private Configuration loadConf(String sourceFolder) throws IOException {
        final Path path = Path.of("src/test/resources/" + sourceFolder + "/configuration.yml");
        final String yaml = Files.readString(path);
        return new ObjectMapper(new YAMLFactory()).readValue(yaml, Configuration.class);
    }
}