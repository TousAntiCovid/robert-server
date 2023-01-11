package fr.gouv.stopc.robertserver.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Return a Slf4j logger for the current class.
 */
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)
