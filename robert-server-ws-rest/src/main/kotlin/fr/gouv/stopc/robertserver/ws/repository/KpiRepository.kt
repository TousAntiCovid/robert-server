package fr.gouv.stopc.robertserver.ws.repository

import fr.gouv.stopc.robertserver.ws.repository.model.Kpi
import fr.gouv.stopc.robertserver.ws.repository.model.KpiName
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

/**
 * Stores KPIs about application usages.
 */
@Repository
interface KpiRepository : MongoRepository<Kpi, String>, KpiCustomRepository {
    fun findByNameIn(names: List<String>): List<Kpi>
}

interface KpiCustomRepository {
    fun incrementKpi(name: KpiName)
}

class KpiCustomRepositoryImpl(private val mongoOperations: MongoOperations) : KpiCustomRepository {

    override fun incrementKpi(name: KpiName) {
        mongoOperations.findAndModify(
            Query.query(Criteria.where("name").`is`(name.key)),
            Update().inc("value", 1),
            FindAndModifyOptions.options()
                .upsert(true)
                .returnNew(true),
            Kpi::class.java
        )
    }
}
