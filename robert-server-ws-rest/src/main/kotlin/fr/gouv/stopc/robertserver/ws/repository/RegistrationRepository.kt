package fr.gouv.stopc.robertserver.ws.repository

import fr.gouv.stopc.robertserver.ws.repository.model.Registration
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import java.time.Duration

interface RegistrationRepository : ReactiveMongoRepository<Registration, ByteArray>, RegistrationCustomRepository

interface RegistrationCustomRepository {
    suspend fun updateLastTimestampDrift(idA: ByteArray, driftTime: Duration)
}

class RegistrationCustomRepositoryImpl(private val mongoOperations: ReactiveMongoOperations) :
    RegistrationCustomRepository {

    override suspend fun updateLastTimestampDrift(idA: ByteArray, driftTime: Duration) {
        mongoOperations.update(Registration::class.java)
            .matching(Query.query(Registration::permanentIdentifier isEqualTo idA))
            .apply(Update.update("lastTimestampDrift", driftTime.seconds))
            .first()
            .awaitSingleOrNull()
    }
}
