package org.kie.akrivis.quarkus;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProcessorCardTest extends ProcessorTestBase {

    @Test
    @TestTransaction
    public void theTest() throws IOException {
        before("http://test.com", "card");

        yardServiceMock.setResult(
                """
                            {"Result": 100}
                        """);

        final long cardId = assertOneCardNoResultHistory();

        processor.scores();

        final Map<String, Object> payload = yardServiceMock.getPayload();
        assertPayload(payload);
        assertInputContainsKeys(payload, "Number of Jira Issues");

        assertEquals(1, resultRepository.history(cardId).size());
        assertTrue(resultRepository.latest(cardId).isPresent());
    }


}