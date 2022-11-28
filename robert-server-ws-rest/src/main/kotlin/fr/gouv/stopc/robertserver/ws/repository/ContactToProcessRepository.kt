package fr.gouv.stopc.robertserver.ws.repository

import fr.gouv.stopc.robertserver.ws.repository.model.ContactToProcess
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ContactToProcessRepository : MongoRepository<ContactToProcess, String>
