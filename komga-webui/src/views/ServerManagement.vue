<template>
  <v-container fluid class="pa-6">
    <v-row>
      <v-col><span class="text-h5">{{ $t('server.server_management.section_title') }}</span></v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="downloadLogFile"
        >{{ $t('server.server_management.download_log') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="cancelAllTasks"
               color="warning"
        >{{ $t('server.server_management.button_cancel_all_tasks') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="modalOptimizeAndRestart = true"
               color="primary"
               :disabled="restarting"
               :loading="restarting"
        >{{ $t('server.server_management.button_optimize_and_restart') }}
        </v-btn>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="auto">
        <v-btn @click="modalStopServer = true"
               color="error"
        >{{ $t('server.server_management.button_shutdown') }}
        </v-btn>
      </v-col>
    </v-row>

    <confirmation-dialog
      v-model="modalStopServer"
      :title="$t('dialog.server_stop.dialog_title')"
      :body="$t('dialog.server_stop.confirmation_message')"
      :button-confirm="$t('dialog.server_stop.button_confirm')"
      button-confirm-color="error"
      @confirm="stopServer"
    />

    <confirmation-dialog
      v-model="modalOptimizeAndRestart"
      :title="$t('dialog.server_optimize_restart.dialog_title')"
      :body="$t('dialog.server_optimize_restart.confirmation_message')"
      :button-confirm="$t('dialog.server_optimize_restart.button_confirm')"
      button-confirm-color="primary"
      @confirm="optimizeAndRestart"
    />

  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import {ERROR, ErrorEvent, NOTIFICATION, NotificationEvent} from '@/types/events'
import jsFileDownloader from 'js-file-downloader'
import urls from '@/functions/urls'

export default Vue.extend({
  name: 'ServerManagement',
  components: {ConfirmationDialog},
  data: () => ({
    modalStopServer: false,
    modalOptimizeAndRestart: false,
    restarting: false,
  }),
  methods: {
    async cancelAllTasks() {
      const count = await this.$komgaTasks.deleteAllTasks()
      this.$eventHub.$emit(NOTIFICATION, {
        message: this.$tc('server.server_management.notification_tasks_cancelled', count),
      } as NotificationEvent)
    },
    async stopServer() {
      try {
        await this.$actuator.shutdown()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
    async optimizeAndRestart() {
      if (this.restarting) return
      this.restarting = true
      try {
        await this.$komgaServer.optimizeAndRestart()
      } catch (e) {
        this.restarting = false
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
        return
      }
      this.$eventHub.$emit(NOTIFICATION, {
        message: this.$t('server.server_management.notification_optimize_restart_started'),
      } as NotificationEvent)
      // wait for the server to go down (vacuum may take a long time), then poll until it's back up
      const shutdownDeadline = Date.now() + 30 * 60 * 1000
      let wentDown = false
      while (Date.now() < shutdownDeadline) {
        await new Promise(resolve => setTimeout(resolve, 3000))
        if (!await this.$komgaServer.isUp()) {
          wentDown = true
          break
        }
      }
      let restarted = false
      if (wentDown) {
        const startupDeadline = Date.now() + 5 * 60 * 1000
        while (Date.now() < startupDeadline) {
          await new Promise(resolve => setTimeout(resolve, 2000))
          if (await this.$komgaServer.isUp()) {
            restarted = true
            break
          }
        }
      }
      this.restarting = false
      if (restarted) {
        this.$eventHub.$emit(NOTIFICATION, {
          message: this.$t('server.server_management.notification_optimize_restart_done'),
        } as NotificationEvent)
        setTimeout(() => location.reload(), 3000)
      } else {
        this.$eventHub.$emit(ERROR, {
          message: this.$t('server.server_management.notification_optimize_restart_timeout'),
        } as ErrorEvent)
      }
    },
    downloadLogFile() {
      new jsFileDownloader({
        url: `${urls.originNoSlash}${this.$actuator.logfileUrl()}`,
        filename: 'komga.log',
        withCredentials: true,
        forceDesktopMode: true,
      })
    },
  },
})
</script>

<style scoped>

</style>
