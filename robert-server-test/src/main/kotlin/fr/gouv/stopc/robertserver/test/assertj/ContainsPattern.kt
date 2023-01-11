package fr.gouv.stopc.robertserver.test.assertj

import org.assertj.core.api.HamcrestCondition.matching
import org.assertj.core.api.ListAssert
import org.hamcrest.Matchers.matchesPattern

fun ListAssert<String>.containsPattern(pattern: String): ListAssert<String> =
    this.haveExactly(
        1,
        matching(matchesPattern(pattern))
    )
