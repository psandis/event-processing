package com.eventprocessing.engine.config;

import com.eventprocessing.common.mapping.PipelineDefinition;
import com.eventprocessing.engine.executor.MappingExecutor;
import com.eventprocessing.engine.stream.TransformTopology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class EngineConfig {

    @Bean
    public MappingExecutor mappingExecutor() {
        return new MappingExecutor();
    }

    @Bean
    public TransformTopology transformTopology(MappingExecutor mappingExecutor) {
        return new TransformTopology(mappingExecutor);
    }

    @Bean
    public PipelineDefinition pipelineDefinition(PipelineLoader pipelineLoader) {
        return pipelineLoader.load();
    }
}
