package org.kie.akrivis.quarkus;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import org.akrivis.dbmodel.RunResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ProcessorReportingTest extends ProcessorTestBase {

    @Test
    @TestTransaction
    public void theTest() throws IOException {
        before("http://test2.com", "reporting/simple");

        yardServiceMock.setResult("""
                    { "Result": [100, 120, 130] }
                """);

        final long cardId = assertOneCardNoResultHistory();

        processor.scores();

        final Map<String, Object> payload = yardServiceMock.getPayload();
        assertPayload(payload);
        assertInputContainsKeys(payload, "Number of Jira Issues");
        assertTrue(((Map) payload.get("input")).get("Number of Jira Issues") instanceof Collection);

        assertEquals(1, resultRepository.history(cardId).size());
        final Optional<RunResult> latest = resultRepository.latest(cardId);
        assertTrue(latest.isPresent());
        assertEquals("[ 100, 120, 130 ]", latest.get().measureValue);
    }
}