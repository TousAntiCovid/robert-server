package fr.gouv.stopc.robertserver.test

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.MAP
import org.assertj.core.api.IterableAssert
import org.assertj.core.api.MapAssert
import org.bson.Document
import org.bson.types.Binary
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import org.springframework.lang.NonNull
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

class MongodbManager : TestExecutionListener {

    override fun beforeTestMethod(@NonNull testContext: TestContext) {
        givenMongodbIsOnline()
        mongoDatabase.drop()
    }

    companion object {
        private val MONGO_DB_CONTAINER = MongoDBContainer(DockerImageName.parse("mongo:4.2.11"))
            .withReuse(true)
            .apply { start() }
            .also {
                val rsUrl = it.getReplicaSetUrl("robert")
                System.setProperty("spring.data.mongodb.uri", "$rsUrl?connectTimeoutMS=1000&socketTimeoutMS=1000")
            }

        private var mongoDatabase = KMongo.createClient(MONGO_DB_CONTAINER.connectionString)
            .getDatabase("robert")

        fun givenRegistrationExistsForIdA(idA: String) = mongoDatabase.getCollection("idTable")
            .insertOne(Document("_id", idA.toByteArray()))

        fun givenRegistrationExistsForIdA(idA: String, vararg epochExpositions: MongoEpochExposition) =
            mongoDatabase.getCollection("idTable")
                .insertOne(
                    Document("_id", idA.toByteArray())
                        .append(
                            "exposedEpochs",
                            epochExpositions.map { (epochId, scores) ->
                                mapOf(
                                    "epochId" to epochId,
                                    "expositionScores" to scores
                                )
                            }
                        )
                )

        fun givenRegistrationExistsForIdA(idA: String, attributes: Map<String, Any>) =
            mongoDatabase.getCollection("idTable")
                .insertOne(
                    Document(attributes)
                        .append("_id", idA.toByteArray())
                )

        fun givenKpiExists(name: String, value: Any) {
            mongoDatabase.getCollection("kpis")
                .insertOne(
                    Document("name", name)
                        .append("value", value)
                )
        }

        /**
         * Ensure the container is not "paused", "unpausing" it if necessary.
         */
        private fun givenMongodbIsOnline() {
            val docker = MONGO_DB_CONTAINER.dockerClient
            val mongodbContainerInspect = docker.inspectContainerCmd(MONGO_DB_CONTAINER.containerId)
                .exec()
            if (mongodbContainerInspect.state.paused == true) {
                docker.unpauseContainerCmd(MONGO_DB_CONTAINER.containerId)
                    .exec()
            }
        }

        /**
         * Put the container in "paused" state. Can be used to simulate failure.
         */
        fun givenMongodbIsOffline() {
            MONGO_DB_CONTAINER.dockerClient
                .pauseContainerCmd(MONGO_DB_CONTAINER.containerId)
                .exec()
        }

        fun assertThatRegistrationCollection(): IterableAssert<Document> {
            val registrations = mongoDatabase.getCollection("idTable").find()
                .map { it.append("permanentIdentifier", it.get("_id", Binary::class.java).data.decodeToString()) }
            return assertThat(registrations)
                .describedAs("List of all registrations in 'idTable' collection")
        }

        fun assertThatRegistrationForIdA(idA: String): MapAssert<Any, Any> = assertThatRegistrationCollection()
            .describedAs("Registration for idA '$idA'")
            .filteredOn { it["permanentIdentifier"] == idA }
            .singleElement(MAP)

        fun assertThatContactsToProcessCollection(): IterableAssert<MongoContactToProcess> {
            val contactsToProcess = mongoDatabase
                .getCollection<MongoContactToProcess>("CONTACTS_TO_PROCESS")
                .find()
            return assertThat(contactsToProcess)
                .describedAs("List of all registrations in 'CONTACTS_TO_PROCESS' collection")
        }

        fun flattenHelloMessage(contact: MongoContactToProcess) = contact.messageDetails?.map { hello ->
            HelloMessageToProcess(
                ebid = contact.ebid.decodeToString(),
                ecc = contact.ecc.decodeToString(),
                time = hello.timeFromHelloMessage,
                mac = hello.mac.decodeToString(),
                rssi = hello.rssiCalibrated,
                receptionTime = hello.timeCollectedOnDevice
            )
        }
    }

    data class MongoContactToProcess(
        val ebid: ByteArray,
        val ecc: ByteArray,
        val messageDetails: List<MongoMessageDetails>?
    )

    data class MongoMessageDetails(
        val timeCollectedOnDevice: Long,
        val timeFromHelloMessage: Int,
        val mac: ByteArray,
        val rssiCalibrated: Int
    )

    data class HelloMessageToProcess(
        val ebid: String,
        val ecc: String,
        val time: Int,
        val mac: String,
        val rssi: Int,
        val receptionTime: Long
    )

    data class MongoEpochExposition(
        val epochId: Int,
        val expositionScores: List<Double>
    )
}
