package org.gotson.komga.infrastructure.util

const val RESTART_PARENT_PID_ENV = "KOMGA_RESTART_PARENT_PID"

fun waitForRestartParent() {
  val parentPid = System.getenv(RESTART_PARENT_PID_ENV)?.toLongOrNull() ?: return
  val deadline = System.currentTimeMillis() + 120_000
  while (System.currentTimeMillis() < deadline) {
    val parentAlive = ProcessHandle.of(parentPid).map { it.isAlive }.orElse(false)
    if (!parentAlive) break
    Thread.sleep(500)
  }
}
