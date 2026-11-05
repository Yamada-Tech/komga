package org.gotson.komga.infrastructure.security

import io.github.oshai.kotlinlogging.KotlinLogging
import org.gotson.komga.domain.persistence.KomgaUserRepository
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class LoginListener(
  private val userRepository: KomgaUserRepository,
) {
  @EventListener
  fun onSuccess(event: AuthenticationSuccessEvent) {
    val komgaPrincipal = event.authentication.principal as KomgaPrincipal
    val user = komgaPrincipal.user

    logger.debug { "Successful authentication for user ${user.email}" }
    userRepository.updateLastLoginDate(user.id)
  }
}
