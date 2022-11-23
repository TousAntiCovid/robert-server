package fr.gouv.stopc.robertserver.ws.controller

import fr.gouv.stopc.robertserver.common.RobertClock
import fr.gouv.stopc.robertserver.ws.api.ClockApi
import fr.gouv.stopc.robertserver.ws.api.model.ClockResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneOffset.UTC

@RestController
class ClockController(private val clock: RobertClock) : ClockApi {

    override fun clock(): ResponseEntity<ClockResponse> {
        val now = clock.now()
        return ResponseEntity.ok(
            ClockResponse(
                time = now.asInstant().atOffset(UTC),
                epoch = now.asEpochId(),
                serviceStartDate = clock.atEpoch(0).asInstant().atZone(UTC).toLocalDate()
            )
        )
    }
}
