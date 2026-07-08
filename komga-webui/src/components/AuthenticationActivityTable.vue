<template>
  <div>
    <v-data-table
      :headers="headers"
      :items="items"
      :options.sync="options"
      :server-items-length="totalItems"
      :loading="loading"
      sort-by="dateTime"
      :sort-desc="true"
      multi-sort
      show-group-by
      class="elevation-1"
      :footer-props="{
          itemsPerPageOptions: [20, 50, 100]
        }"
    >
      <template v-slot:item.success="{ item }">
        <v-icon v-if="item.success" color="success">mdi-check-circle</v-icon>
        <v-icon v-else color="error">mdi-alert-circle</v-icon>
      </template>

      <template v-slot:item.dateTime="{ item }">
        {{
          new Intl.DateTimeFormat($i18n.locale, {
            dateStyle: 'medium',
            timeStyle: 'short'
          }).format(item.dateTime)
        }}
      </template>
    </v-data-table>

    <confirmation-dialog
      v-model="modalClearHistory"
      :title="$t('authentication_activity.clear_history')"
      :body="$t('authentication_activity.clear_warning')"
      :button-confirm="$t('authentication_activity.clear_confirm')"
      button-confirm-color="warning"
      @confirm="doClearHistory"
    />
  </div>
</template>

<script lang="ts">
import Vue from 'vue'
import {ERROR} from '@/types/events'
import {AuthenticationActivityDto} from '@/types/komga-users'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'

export default Vue.extend({
  name: 'AuthenticationActivityTable',
  components: {ConfirmationDialog},
  data: function () {
    return {
      items: [] as AuthenticationActivityDto[],
      totalItems: 0,
      loading: true,
      options: {} as any,
      modalClearHistory: false,
    }
  },
  props: {
    forMe: {
      type: Boolean,
      default: false,
    },
  },
  watch: {
    options: {
      handler() {
        this.loadData()
      },
      deep: true,
    },
  },
  computed: {
    headers(): object[] {
      const headers = []
      if (!this.forMe) headers.push({text: this.$t('authentication_activity.email').toString(), value: 'email'})
      headers.push(
        {text: this.$t('authentication_activity.ip').toString(), value: 'ip'},
        {text: this.$t('authentication_activity.user_agent').toString(), value: 'userAgent'},
        {text: this.$t('authentication_activity.success').toString(), value: 'success'},
        {text: this.$t('authentication_activity.source').toString(), value: 'source'},
        {text: this.$t('authentication_activity.api_key').toString(), value: 'apiKeyComment'},
        {text: this.$t('authentication_activity.error').toString(), value: 'error'},
        {text: this.$t('authentication_activity.datetime').toString(), value: 'dateTime', groupable: false},
      )
      return headers
    },
  },
  methods: {
    async loadData() {
      this.loading = true

      const {sortBy, sortDesc, page, itemsPerPage} = this.options

      const pageRequest = {
        page: page - 1,
        size: itemsPerPage,
        sort: [],
      } as PageRequest

      for (let i = 0; i < sortBy.length; i++) {
        pageRequest.sort!!.push(`${sortBy[i]},${sortDesc[i] ? 'desc' : 'asc'}`)
      }

      let itemsPage
      try {
        if (!this.forMe) itemsPage = await this.$komgaUsers.getAuthenticationActivity(pageRequest)
        else itemsPage = await this.$komgaUsers.getMyAuthenticationActivity(pageRequest)
        this.totalItems = itemsPage.totalElements
        this.items = itemsPage.content
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }

      this.loading = false
    },
    promptClearHistory() {
      this.modalClearHistory = true
    },
    async doClearHistory() {
      try {
        await this.$komgaUsers.clearAuthenticationActivity()
        this.items = []
        this.totalItems = 0
        if (this.options.page !== 1) {
          this.options = {...this.options, page: 1}
        } else {
          await this.loadData()
        }
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
  },
})
</script>
