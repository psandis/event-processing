package com.eventprocessing.ingest.service;

import com.eventprocessing.common.model.Event;
import com.eventprocessing.common.model.EventRequest;

public interface EventSubmitter {

    Event submit(EventRequest request);
}
