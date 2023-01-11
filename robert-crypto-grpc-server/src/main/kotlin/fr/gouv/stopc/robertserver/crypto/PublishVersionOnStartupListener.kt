package fr.gouv.stopc.robertserver.crypto

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.info.BuildProperties
import org.springframework.boot.info.GitProperties
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class PublishVersionOnStartupListener(
    private val meterRegistry: MeterRegistry,
    private val buildProperties: BuildProperties,
    private val gitProperties: GitProperties
) {
    @EventListener
    fun publishVersion(event: ApplicationStartedEvent) {
        Counter.builder("app")
            .description("Informations about application")
            .tag("version", buildProperties.version)
            .tag("commit", gitProperties.shortCommitId)
            .register(meterRegistry)
            .increment()
    }
}
