package org.kie.akrivis.scheduler.responses;

import org.akrivis.dbmodel.result.Threshold;

import java.util.Map;

public record Result(
        Long cardId,
        Long runTime,
        String status,
        Map measureValue,
        String measureName,
        Integer maxValue,
        String yaml,
        Threshold[] thresholds

){}
