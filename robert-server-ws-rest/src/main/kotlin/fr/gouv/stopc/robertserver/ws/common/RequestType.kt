package fr.gouv.stopc.robertserver.ws.common

/**
 * Robert protocol request type value.
 *
 * @see @see <a href="https://github.com/ROBERT-proximity-tracing/documents/blob/master/ROBERT-specification-EN-v1_1.pdf">Robert protocol</a> C. Authenticated Requests
 */
enum class RequestType(val code: Int) {
    HELLO(1),
    STATUS(2),
    UNREGISTER(3),
    DELETE_EXPOSURE_HISTORY(4),
}
