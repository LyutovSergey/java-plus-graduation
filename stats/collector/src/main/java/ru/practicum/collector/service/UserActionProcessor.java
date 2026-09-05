package ru.practicum.collector.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.collector.mapper.UserActionMapper;
import ru.practicum.collector.service.kafka.KafkaEventProducer;
import ru.practicum.stats.proto.UserActionProto;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionProcessor {

    private final UserActionMapper mapper;
    private final KafkaEventProducer producer;

    public void processEvent(UserActionProto request) {
        log.debug("Processing user action: userId={}, eventId={}", request.getUserId(), request.getEventId());
        var avroEvent = mapper.toAvro(request);
        producer.send(avroEvent);
    }
}