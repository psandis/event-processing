package com.eventprocessing.admin.service;

import com.eventprocessing.common.discovery.SchemaDiscovery;
import com.eventprocessing.common.discovery.SchemaDiscovery.FieldInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TopicService {

    private static final Logger log = LoggerFactory.getLogger(TopicService.class);
    private static final int SAMPLE_SIZE = 10;

    private final String bootstrapServers;
    private final ObjectMapper objectMapper;
    private final SchemaDiscovery schemaDiscovery;

    public TopicService(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                        ObjectMapper objectMapper) {
        this.bootstrapServers = bootstrapServers;
        this.objectMapper = objectMapper;
        this.schemaDiscovery = new SchemaDiscovery();
    }

    public List<String> listTopics() {
        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            Set<String> topics = consumer.listTopics().keySet();
            return topics.stream()
                    .filter(t -> !t.startsWith("__"))
                    .sorted()
                    .toList();
        }
    }

    public List<JsonNode> getSampleEvents(String topic, int count) {
        int sampleCount = Math.min(count, 100);
        List<JsonNode> samples = new ArrayList<>();

        try (KafkaConsumer<String, String> consumer = createConsumer()) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();

            consumer.assign(partitions);
            consumer.seekToEnd(partitions);

            for (TopicPartition partition : partitions) {
                long endOffset = consumer.position(partition);
                long startOffset = Math.max(0, endOffset - sampleCount);
                consumer.seek(partition, startOffset);
            }

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode node = objectMapper.readTree(record.value());
                    samples.add(node);
                    if (samples.size() >= sampleCount) break;
                } catch (Exception e) {
                    log.debug("Skipping non-JSON record from topic {}", topic);
                }
            }
        }

        return samples;
    }

    public Map<String, FieldInfo> discoverSchema(String topic) {
        List<JsonNode> samples = getSampleEvents(topic, SAMPLE_SIZE);

        List<JsonNode> payloads = samples.stream()
                .map(event -> event.has("payload") ? event.get("payload") : event)
                .toList();

        return schemaDiscovery.discoverFromMultiple(payloads);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "event-admin-discovery-" + System.currentTimeMillis());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return new KafkaConsumer<>(props);
    }
}
