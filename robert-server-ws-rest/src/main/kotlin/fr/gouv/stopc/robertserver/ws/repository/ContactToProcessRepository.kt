package fr.gouv.stopc.robertserver.ws.repository

import fr.gouv.stopc.robertserver.ws.repository.model.ContactToProcess
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface ContactToProcessRepository : ReactiveMongoRepository<ContactToProcess, String>
