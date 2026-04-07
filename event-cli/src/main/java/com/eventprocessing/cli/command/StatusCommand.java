package com.eventprocessing.cli.command;

import com.eventprocessing.cli.EventCli;
import com.eventprocessing.cli.client.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

@Command(name = "status", description = "Platform health status", mixinStandardHelpOptions = true)
public class StatusCommand implements Callable<Integer> {

    @ParentCommand EventCli cli;

    @Override
    public Integer call() {
        ApiClient client = new ApiClient(cli.adminUrl, cli.ingestUrl);

        System.out.println("Event Processing Platform");
        System.out.println("=".repeat(40));

        try {
            JsonNode admin = client.adminStatus();
            System.out.printf("  Admin:  %s (%s)%n", admin.get("status").asText(), cli.adminUrl);
        } catch (Exception e) {
            System.out.printf("  Admin:  DOWN (%s)%n", cli.adminUrl);
        }

        try {
            JsonNode ingest = client.ingestHealth();
            System.out.printf("  Ingest: %s (%s)%n", ingest.get("status").asText(), cli.ingestUrl);
        } catch (Exception e) {
            System.out.printf("  Ingest: DOWN (%s)%n", cli.ingestUrl);
        }

        try {
            var topics = client.listTopics();
            System.out.printf("  Topics: %d available%n", topics.size());
        } catch (Exception e) {
            System.out.println("  Topics: unavailable");
        }

        try {
            var pipelines = client.listPipelines();
            long active = pipelines.stream().filter(p -> "ACTIVE".equals(p.get("state").asText())).count();
            long draft = pipelines.stream().filter(p -> "DRAFT".equals(p.get("state").asText())).count();
            System.out.printf("  Pipelines: %d total (%d active, %d draft)%n", pipelines.size(), active, draft);
        } catch (Exception e) {
            System.out.println("  Pipelines: unavailable");
        }

        return 0;
    }
}
