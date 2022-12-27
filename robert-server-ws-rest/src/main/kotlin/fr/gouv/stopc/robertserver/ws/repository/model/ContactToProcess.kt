package fr.gouv.stopc.robertserver.ws.repository.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "CONTACTS_TO_PROCESS")
data class ContactToProcess(

    @Id
    val id: String = UUID.randomUUID().toString(),
    val ebid: List<Byte>,
    val ecc: List<Byte>,
    val messageDetails: List<HelloMessageDetail>
)

data class HelloMessageDetail(

    /**
     * WARNING: Since Java does not support unsigned int, we are obligated to store the value coming from the JSON representation
     * as a Long even though it should be used as an unsigned int.
     */
    val timeCollectedOnDevice: Long,

    /**
     * WARNING: Since Java does not support unsigned short, we are obligated to store the value coming from the JSON
     * representation as an Integer even though it should be used as an unsigned short.
     */
    val timeFromHelloMessage: Int,

    val mac: List<Byte>,

    val rssiCalibrated: Int
)
