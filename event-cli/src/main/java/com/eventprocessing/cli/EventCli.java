package com.eventprocessing.cli;

import com.eventprocessing.cli.command.EventsCommand;
import com.eventprocessing.cli.command.PipelinesCommand;
import com.eventprocessing.cli.command.StatusCommand;
import com.eventprocessing.cli.command.TopicsCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "ep",
        description = "Event Processing Platform CLI",
        mixinStandardHelpOptions = true,
        version = "0.0.6",
        subcommands = {
                PipelinesCommand.class,
                TopicsCommand.class,
                EventsCommand.class,
                StatusCommand.class,
        }
)
public class EventCli {

    @Option(names = {"--admin-url"}, defaultValue = "http://localhost:8091",
            description = "Admin API URL (default: ${DEFAULT-VALUE})")
    public String adminUrl;

    @Option(names = {"--ingest-url"}, defaultValue = "http://localhost:8090",
            description = "Ingest API URL (default: ${DEFAULT-VALUE})")
    public String ingestUrl;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new EventCli())
                .setExecutionStrategy(new CommandLine.RunLast())
                .execute(args);
        System.exit(exitCode);
    }
}
