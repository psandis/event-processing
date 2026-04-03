package com.eventprocessing.common.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventRequest(

        @NotBlank
        String type,

        @NotBlank
        String source,

        @NotNull
        JsonNode payload,

        JsonNode metadata
) {
}
