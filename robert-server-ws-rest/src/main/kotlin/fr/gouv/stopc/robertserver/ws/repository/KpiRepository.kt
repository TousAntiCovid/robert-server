package fr.gouv.stopc.robertserver.ws.repository

import fr.gouv.stopc.robertserver.ws.repository.model.Kpi
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

/**
 * Stores KPIs about application usages.
 */
interface KpiRepository : ReactiveMongoRepository<Kpi, String>, KpiCustomRepository {
    fun findByNameIn(names: List<String>): Flux<Kpi>
}

interface KpiCustomRepository {
    suspend fun incrementKpi(name: KpiName)
}

class KpiCustomRepositoryImpl(private val mongoOperations: ReactiveMongoOperations) : KpiCustomRepository {

    override suspend fun incrementKpi(name: KpiName) {
        mongoOperations.findAndModify(
            Query.query(Criteria.where("name").`is`(name.key)),
            Update().inc("value", 1),
            FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true),
            Kpi::class.java
        ).awaitSingle()
    }
}
