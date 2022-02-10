package fr.gouv.stopc.e2e.steps;

import io.cucumber.java.fr.Etantdonnéque;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class RobertPlatformTime {

    @Etantdonnéque("l'on est aujourd'hui")
    public void resetFakeTimeToNow() throws IOException {
        new ProcessBuilder()
                .command(execInContainer("ws-rest", "rm -f /etc/faketime.d/faketime"))
                .start();
    }

    @Etantdonnéque("l'on est il y a {duration}")
    public void changeSystemDateTo(final Duration duration) throws IOException {

        new ProcessBuilder()
                .command(execInContainer("ws-rest", "echo " + duration.toSeconds() + " > /faketimeDir/faketime"))
                .start();

        // verify date on each container
    }

    private List<String> execInContainer(final String containerName, final String command) {
        return List.of("docker-compose", "exec", "-T", containerName, "bash", "-c", command);
    }
}
