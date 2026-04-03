package com.eventprocessing.common.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldMapping {

    private String sourceField;
    private String destinationField;
    private ConversionType conversion;
    private String defaultValue;
    private String format;
    private boolean excluded;

    public FieldMapping() {
    }

    public FieldMapping(String sourceField, String destinationField) {
        this.sourceField = sourceField;
        this.destinationField = destinationField;
    }

    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }

    public String getDestinationField() { return destinationField; }
    public void setDestinationField(String destinationField) { this.destinationField = destinationField; }

    public ConversionType getConversion() { return conversion; }
    public void setConversion(ConversionType conversion) { this.conversion = conversion; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public boolean isExcluded() { return excluded; }
    public void setExcluded(boolean excluded) { this.excluded = excluded; }
}
