package com.eventprocessing.cli.command;

import com.eventprocessing.cli.EventCli;
import com.eventprocessing.cli.client.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "events", description = "Submit events", mixinStandardHelpOptions = true, subcommands = {
        EventsCommand.SendCmd.class,
})
public class EventsCommand {

    @ParentCommand EventCli cli;

    @Command(name = "send", description = "Submit an event", mixinStandardHelpOptions = true)
    static class SendCmd implements Callable<Integer> {
        @ParentCommand EventsCommand parent;

        @Option(names = "--type", required = true, description = "Event type") String type;
        @Option(names = "--source", required = true, description = "Event source") String source;
        @Option(names = "--payload", description = "JSON payload (inline)") String payload;
        @Option(names = "--file", description = "JSON payload from file") Path file;

        @Override
        public Integer call() throws Exception {
            String payloadJson;
            if (file != null) {
                payloadJson = Files.readString(file);
            } else if (payload != null) {
                payloadJson = payload;
            } else {
                System.err.println("Either --payload or --file is required.");
                return 1;
            }

            String body = """
                    {"type":"%s","source":"%s","payload":%s}
                    """.formatted(type, source, payloadJson);

            ApiClient client = new ApiClient(parent.cli.adminUrl, parent.cli.ingestUrl);
            JsonNode result = client.submitEvent(body);

            System.out.printf("Event submitted: %s (%s)%n",
                    result.get("id").asText(),
                    result.get("status").asText());
            return 0;
        }
    }
}
