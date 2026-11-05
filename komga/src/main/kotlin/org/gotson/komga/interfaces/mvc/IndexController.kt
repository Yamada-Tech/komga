package org.gotson.komga.interfaces.mvc

import jakarta.servlet.ServletContext
import org.gotson.komga.infrastructure.configuration.KomgaSettingsProvider
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController(
  servletContext: ServletContext,
  val komgaSettingsProvider: KomgaSettingsProvider,
) {
  private val baseUrl: String = "${servletContext.contextPath}/"

  private fun addBranding(model: Model) {
    model.addAttribute("baseUrl", baseUrl)
    model.addAttribute("siteName", komgaSettingsProvider.siteName)
    model.addAttribute("logoAvailable", komgaSettingsProvider.logoImage != null)
  }

  @GetMapping("/")
  fun index(
    model: Model,
  ): String {
    addBranding(model)

    return if (komgaSettingsProvider.nextUiAsDefault) "index-next" else "index"
  }

  @GetMapping(value = ["/classic", "/classic/**"])
  fun indexClassic(model: Model): String {
    addBranding(model)
    return "index"
  }

  @GetMapping(value = ["/next", "/next/**"])
  fun indexNext(model: Model): String {
    addBranding(model)
    return "index-next"
  }
}
