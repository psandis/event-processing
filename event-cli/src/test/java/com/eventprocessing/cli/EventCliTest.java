package com.eventprocessing.cli;

import picocli.CommandLine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventCliTest {

    @Test
    void helpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void versionReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("--version");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void pipelinesHelpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("pipelines", "--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void topicsHelpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("topics", "--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void eventsHelpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("events", "--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void statusHelpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("status", "--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void unknownCommandReturnsError() {
        int exitCode = new CommandLine(new EventCli()).execute("nonexistent");
        assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    void pipelinesListHelpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("pipelines", "list", "--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void pipelinesCreateHelpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("pipelines", "create", "--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void eventsSendHelpReturnsZero() {
        int exitCode = new CommandLine(new EventCli()).execute("events", "send", "--help");
        assertThat(exitCode).isEqualTo(0);
    }

    @Test
    void eventsSendMissingRequiredOptions() {
        int exitCode = new CommandLine(new EventCli()).execute("events", "send");
        assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    void pipelinesCreateMissingRequiredOptions() {
        int exitCode = new CommandLine(new EventCli()).execute("pipelines", "create");
        assertThat(exitCode).isNotEqualTo(0);
    }

    @Test
    void customAdminUrlIsParsed() {
        EventCli cli = new EventCli();
        new CommandLine(cli).parseArgs("--admin-url", "http://custom:9999");
        assertThat(cli.adminUrl).isEqualTo("http://custom:9999");
    }

    @Test
    void customIngestUrlIsParsed() {
        EventCli cli = new EventCli();
        new CommandLine(cli).parseArgs("--ingest-url", "http://custom:8888");
        assertThat(cli.ingestUrl).isEqualTo("http://custom:8888");
    }

    @Test
    void defaultUrls() {
        EventCli cli = new EventCli();
        new CommandLine(cli).parseArgs();
        assertThat(cli.adminUrl).isEqualTo("http://localhost:8091");
        assertThat(cli.ingestUrl).isEqualTo("http://localhost:8090");
    }
}
