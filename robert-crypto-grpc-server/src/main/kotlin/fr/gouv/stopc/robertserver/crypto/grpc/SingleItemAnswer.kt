package fr.gouv.stopc.robertserver.crypto.grpc

import com.google.protobuf.GeneratedMessageV3
import fr.gouv.stopc.robertserver.common.logger
import io.grpc.stub.StreamObserver

inline fun <V : GeneratedMessageV3> StreamObserver<V>.singleItemAnswer(handler: () -> V) {
    try {
        onNext(handler())
        onCompleted()
    } catch (e: Exception) {
        logger().error("An exception occurred while handling response", e)
        onError(e)
    }
}
