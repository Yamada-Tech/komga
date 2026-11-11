package org.gotson.komga.interfaces.api.kobo.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

// Response of the Kobo endpoint mapped from "get_tests_request" (see KoboProxy): an A/B-testing
// payload, not a unit test. The name "Tests" is Kobo domain vocabulary, keep it as is.
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class TestsDto(
  val result: String,
  val testKey: String,
  val tests: Map<String, String> = emptyMap(),
)
