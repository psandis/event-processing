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

@Command(name = "pipelines", description = "Manage pipelines", mixinStandardHelpOptions = true, subcommands = {
        PipelinesCommand.ListCmd.class,
        PipelinesCommand.GetCmd.class,
        PipelinesCommand.CreateCmd.class,
        PipelinesCommand.DeleteCmd.class,
        PipelinesCommand.DeployCmd.class,
        PipelinesCommand.PauseCmd.class,
        PipelinesCommand.ResumeCmd.class,
        PipelinesCommand.DraftCmd.class,
        PipelinesCommand.TestCmd.class,
        PipelinesCommand.VersionsCmd.class,
})
public class PipelinesCommand {

    @Command(name = "list", description = "List all pipelines", mixinStandardHelpOptions = true)
    static class ListCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;

        @Override
        public Integer call() throws Exception {
            ApiClient client = getClient(this);
            List<JsonNode> pipelines = client.listPipelines();

            if (pipelines.isEmpty()) {
                System.out.println("No pipelines found.");
                return 0;
            }

            System.out.printf("%-30s %-8s %-10s %-30s %-30s %-8s%n",
                    "NAME", "VERSION", "STATE", "SOURCE", "DESTINATION", "MAPPINGS");
            System.out.println("-".repeat(120));

            for (JsonNode p : pipelines) {
                System.out.printf("%-30s v%-7d %-10s %-30s %-30s %d%n",
                        p.get("name").asText(),
                        p.get("version").asInt(),
                        p.get("state").asText(),
                        p.get("sourceTopic").asText(),
                        p.get("destinationTopic").asText(),
                        p.get("fieldMappings").size());
            }
            return 0;
        }
    }

    @Command(name = "get", description = "Get pipeline details", mixinStandardHelpOptions = true)
    static class GetCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;

        @Override
        public Integer call() throws Exception {
            JsonNode p = getClient(this).getPipeline(name);
            System.out.println(getClient(this).mapper().writerWithDefaultPrettyPrinter().writeValueAsString(p));
            return 0;
        }
    }

    @Command(name = "create", description = "Create a new pipeline", mixinStandardHelpOptions = true)
    static class CreateCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Option(names = "--name", required = true, description = "Pipeline name") String name;
        @Option(names = "--source", required = true, description = "Source topic") String source;
        @Option(names = "--dest", required = true, description = "Destination topic") String dest;
        @Option(names = "--description", defaultValue = "", description = "Description") String description;

        @Override
        public Integer call() throws Exception {
            String body = """
                    {"name":"%s","description":"%s","sourceTopic":"%s","destinationTopic":"%s","fieldMappings":[]}
                    """.formatted(name, description, source, dest);
            JsonNode result = getClient(this).createPipeline(body);
            System.out.printf("Created pipeline: %s v%d (%s)%n",
                    result.get("name").asText(),
                    result.get("version").asInt(),
                    result.get("state").asText());
            return 0;
        }
    }

    @Command(name = "delete", description = "Delete a pipeline", mixinStandardHelpOptions = true)
    static class DeleteCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;

        @Override
        public Integer call() throws Exception {
            getClient(this).deletePipeline(name);
            System.out.println("Deleted: " + name);
            return 0;
        }
    }

    @Command(name = "deploy", description = "Deploy a pipeline version", mixinStandardHelpOptions = true)
    static class DeployCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;
        @Option(names = "--version", required = true, description = "Version to deploy") int version;

        @Override
        public Integer call() throws Exception {
            JsonNode result = getClient(this).deploy(name, version);
            System.out.printf("Deployed: %s v%d (%s)%n",
                    result.get("name").asText(),
                    result.get("version").asInt(),
                    result.get("state").asText());
            return 0;
        }
    }

    @Command(name = "pause", description = "Pause a pipeline", mixinStandardHelpOptions = true)
    static class PauseCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;

        @Override
        public Integer call() throws Exception {
            JsonNode result = getClient(this).pause(name);
            System.out.printf("Paused: %s v%d%n", result.get("name").asText(), result.get("version").asInt());
            return 0;
        }
    }

    @Command(name = "resume", description = "Resume a pipeline", mixinStandardHelpOptions = true)
    static class ResumeCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;

        @Override
        public Integer call() throws Exception {
            JsonNode result = getClient(this).resume(name);
            System.out.printf("Resumed: %s v%d%n", result.get("name").asText(), result.get("version").asInt());
            return 0;
        }
    }

    @Command(name = "draft", description = "Create a new draft version", mixinStandardHelpOptions = true)
    static class DraftCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;

        @Override
        public Integer call() throws Exception {
            JsonNode result = getClient(this).createDraft(name);
            System.out.printf("Draft created: %s v%d%n", result.get("name").asText(), result.get("version").asInt());
            return 0;
        }
    }

    @Command(name = "test", description = "Test a pipeline mapping with sample data", mixinStandardHelpOptions = true)
    static class TestCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;
        @Parameters(index = "1", description = "JSON payload") String payload;

        @Override
        public Integer call() throws Exception {
            JsonNode result = getClient(this).testMapping(name, payload);
            System.out.println(getClient(this).mapper().writerWithDefaultPrettyPrinter().writeValueAsString(result));
            return 0;
        }
    }

    @Command(name = "versions", description = "List versions of a pipeline", mixinStandardHelpOptions = true)
    static class VersionsCmd implements Callable<Integer> {
        @ParentCommand PipelinesCommand parent;
        @Parameters(index = "0", description = "Pipeline name") String name;

        @Override
        public Integer call() throws Exception {
            List<JsonNode> versions = getClient(this).getPipelineVersions(name);
            System.out.printf("%-8s %-10s %-30s %-30s%n", "VERSION", "STATE", "SOURCE", "DESTINATION");
            System.out.println("-".repeat(80));
            for (JsonNode v : versions) {
                System.out.printf("v%-7d %-10s %-30s %-30s%n",
                        v.get("version").asInt(),
                        v.get("state").asText(),
                        v.get("sourceTopic").asText(),
                        v.get("destinationTopic").asText());
            }
            return 0;
        }
    }

    private static ApiClient getClient(Object cmd) {
        try {
            var parent = cmd.getClass().getDeclaredField("parent");
            parent.setAccessible(true);
            var pipelines = (PipelinesCommand) parent.get(cmd);
            var cliField = PipelinesCommand.class.getDeclaredField("cli");
            cliField.setAccessible(true);
            var cli = (EventCli) cliField.get(pipelines);
            return new ApiClient(cli.adminUrl, cli.ingestUrl);
        } catch (Exception e) {
            return new ApiClient("http://localhost:8091", "http://localhost:8090");
        }
    }

    @ParentCommand EventCli cli;
}
