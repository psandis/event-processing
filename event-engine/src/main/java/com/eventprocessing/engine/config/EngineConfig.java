package com.eventprocessing.engine.config;

import com.eventprocessing.engine.executor.MappingExecutor;
import com.eventprocessing.engine.stream.TransformTopology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EngineConfig {

    @Bean
    public MappingExecutor mappingExecutor() {
        return new MappingExecutor();
    }

    @Bean
    public TransformTopology transformTopology(MappingExecutor mappingExecutor) {
        return new TransformTopology(mappingExecutor);
    }
}
