package fr.gouv.stopc.robertserver.ws.controller;

import fr.gouv.stopc.robert.server.common.service.RobertClock;
import fr.gouv.stopc.robertserver.ws.api.RobertApi;
import fr.gouv.stopc.robertserver.ws.api.model.ClockResponse;
import fr.gouv.stopc.robertserver.ws.utils.UriConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;

@RestController
@RequestMapping("${controller.path.prefix}" + UriConstants.API_V6)
@RequiredArgsConstructor
public class ClockController implements RobertApi {

    private final RobertClock robertClock;

    @Override
    public ResponseEntity<ClockResponse> clock() {
        final var robertInstant = robertClock.now();
        return ResponseEntity.ok(
                ClockResponse.builder()
                        .serviceStartTime(LocalDate.ofInstant(robertClock.getServiceStartTime().asInstant(), UTC))
                        .time(robertInstant.asInstant().atOffset(UTC))
                        .epoch(robertInstant.asEpochId())
                        .build()
        );
    }
}
