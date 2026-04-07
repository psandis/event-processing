package com.eventprocessing.cli.command;

import com.eventprocessing.cli.EventCli;
import com.eventprocessing.cli.client.ApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "topics", description = "Kafka topic operations", mixinStandardHelpOptions = true, subcommands = {
        TopicsCommand.ListCmd.class,
        TopicsCommand.SchemaCmd.class,
        TopicsCommand.SampleCmd.class,
})
public class TopicsCommand {

    @ParentCommand EventCli cli;

    @Command(name = "list", description = "List available Kafka topics", mixinStandardHelpOptions = true)
    static class ListCmd implements Callable<Integer> {
        @ParentCommand TopicsCommand parent;

        @Override
        public Integer call() throws Exception {
            List<String> topics = client(parent).listTopics();
            if (topics.isEmpty()) {
                System.out.println("No topics found.");
            } else {
                topics.forEach(System.out::println);
            }
            return 0;
        }
    }

    @Command(name = "schema", description = "Discover schema from topic", mixinStandardHelpOptions = true)
    static class SchemaCmd implements Callable<Integer> {
        @ParentCommand TopicsCommand parent;
        @Parameters(index = "0", description = "Topic name") String topic;

        @Override
        public Integer call() throws Exception {
            JsonNode schema = client(parent).discoverSchema(topic);
            System.out.printf("%-30s %-10s %-12s%n", "FIELD", "TYPE", "OCCURRENCES");
            System.out.println("-".repeat(55));
            schema.fields().forEachRemaining(entry -> {
                JsonNode info = entry.getValue();
                System.out.printf("%-30s %-10s %d%n",
                        info.get("path").asText(),
                        info.get("type").asText(),
                        info.get("occurrences").asInt());
            });
            return 0;
        }
    }

    @Command(name = "sample", description = "Get sample events from a topic", mixinStandardHelpOptions = true)
    static class SampleCmd implements Callable<Integer> {
        @ParentCommand TopicsCommand parent;
        @Parameters(index = "0", description = "Topic name") String topic;
        @Option(names = "--count", defaultValue = "3", description = "Number of samples") int count;

        @Override
        public Integer call() throws Exception {
            List<JsonNode> samples = client(parent).getSampleEvents(topic, count);
            for (int i = 0; i < samples.size(); i++) {
                System.out.printf("--- Event %d ---%n", i + 1);
                System.out.println(client(parent).mapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(samples.get(i)));
            }
            return 0;
        }
    }

    private static ApiClient client(TopicsCommand parent) {
        return new ApiClient(parent.cli.adminUrl, parent.cli.ingestUrl);
    }
}
