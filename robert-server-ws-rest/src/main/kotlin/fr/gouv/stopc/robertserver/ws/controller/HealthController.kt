package fr.gouv.stopc.robertserver.ws.controller

import org.springframework.boot.actuate.autoconfigure.web.server.ConditionalOnManagementPort
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType.DIFFERENT
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Our gateway can only make health request on the same port the service is exposed.
 *
 * This is a proxy to expose the /actuator/health endpoint and keep other management endpoints on a separated port.
 */
@RestController
@ConditionalOnManagementPort(DIFFERENT)
@RequestMapping("/actuator/health")
class HealthController(private val healthEndpoint: HealthEndpoint) {

    @GetMapping
    fun health() = ResponseEntity.ok(healthEndpoint.health().status)
}
