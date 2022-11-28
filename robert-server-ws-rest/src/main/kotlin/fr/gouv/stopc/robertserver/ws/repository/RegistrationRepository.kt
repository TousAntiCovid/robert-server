package fr.gouv.stopc.robertserver.ws.repository

import fr.gouv.stopc.robertserver.ws.repository.model.Registration
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
interface RegistrationRepository : MongoRepository<Registration, ByteArray>, RegistrationCustomRepository

interface RegistrationCustomRepository {
    fun updateLastTimestampDrift(idA: ByteArray, driftTime: Duration)
}

class RegistrationCustomRepositoryImpl(private val mongoOperations: MongoOperations) :
    RegistrationCustomRepository {

    override fun updateLastTimestampDrift(idA: ByteArray, driftTime: Duration) {
        mongoOperations.update(Registration::class.java)
            .matching(Query.query(Registration::permanentIdentifier isEqualTo idA))
            .apply(Update.update("lastTimestampDrift", driftTime.seconds))
            .first()
    }
}
