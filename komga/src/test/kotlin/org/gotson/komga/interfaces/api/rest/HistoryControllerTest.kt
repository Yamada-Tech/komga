package org.gotson.komga.interfaces.api.rest

import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.gotson.komga.domain.persistence.AuthenticationActivityRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@WebMvcTest(HistoryController::class)
@Import(HistoryControllerTest.SecurityTestConfiguration::class)
class HistoryControllerTest
  @Autowired constructor(
    private val mockMvc: MockMvc,
  ) {
    @MockkBean
    private lateinit var authenticationActivityRepository: AuthenticationActivityRepository

    @Test
    fun `admin can clear authentication activity`() {
      every { authenticationActivityRepository.deleteAll() } just Runs

      mockMvc
        .post("/api/v1/history/authentication-activity/clear") {
          with(httpBasic("admin", "password"))
        }.andExpect {
          status { isOk() }
        }

      verify(exactly = 1) { authenticationActivityRepository.deleteAll() }
    }

    @Test
    fun `non-admin is denied clearing authentication activity`() {
      mockMvc
        .post("/api/v1/history/authentication-activity/clear") {
          with(httpBasic("user", "password"))
        }.andExpect {
          status { isForbidden() }
        }

      verify(exactly = 0) { authenticationActivityRepository.deleteAll() }
    }

    @Test
    fun `clearing authentication activity deletes all data`() {
      every { authenticationActivityRepository.deleteAll() } just Runs

      HistoryController(authenticationActivityRepository).clearAuthenticationActivity()

      verify(exactly = 1) { authenticationActivityRepository.deleteAll() }
    }

    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    class SecurityTestConfiguration {
      @Bean
      fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
          .csrf { it.disable() }
          .authorizeHttpRequests { it.anyRequest().authenticated() }
          .httpBasic {}
          .build()

      @Bean
      fun userDetailsService(): UserDetailsService =
        InMemoryUserDetailsManager(
          User.withUsername("admin").password("{noop}password").roles("ADMIN").build(),
          User.withUsername("user").password("{noop}password").roles("USER").build(),
        )
    }
  }
