package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robertserver.common.RobertClock;
import fr.gouv.stopc.robertserver.ws.api.RobertApi;
import fr.gouv.stopc.robertserver.ws.api.model.ClockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.time.ZoneOffset.UTC;

@RestController
@RequestMapping("/api/v6")
@RequiredArgsConstructor
public class ClockController implements RobertApi {

    private final RobertClock robertClock;

    @Override
    public ResponseEntity<ClockResponse> clock() {
        final var robertInstant = robertClock.now();
        return ResponseEntity.ok(
                ClockResponse.builder()
                        .serviceStartDate(robertClock.atEpoch(0).asInstant().atZone(UTC).toLocalDate())
                        .time(robertInstant.asInstant().atOffset(UTC))
                        .epoch(robertInstant.asEpochId())
                        .build()
        );
    }
}
