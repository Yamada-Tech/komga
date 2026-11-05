<template>
  <v-data-table
    :headers="headers"
    :items="items"
    :loading="loading"
    :items-per-page="-1"
    class="elevation-1"
  >
    <template v-slot:item.bookTitle="{ item }">
      <router-link
        :to="{name: 'browse-book', params: {bookId: item.bookId}}"
        class="link-underline"
      >{{ item.bookTitle }}
      </router-link>
    </template>

    <template v-slot:item.seriesTitle="{ item }">
      <router-link
        :to="{name: 'browse-series', params: {seriesId: item.seriesId}}"
        class="link-underline"
      >{{ item.seriesTitle }}
      </router-link>
    </template>

    <template v-slot:item.progress="{ item }">
      <v-icon v-if="item.completed" color="success" small>mdi-check-circle</v-icon>
      <span v-else>{{ item.page }}/{{ item.pagesCount ?? '?' }}</span>
    </template>

    <template v-slot:item.readDate="{ item }">
      {{
        new Intl.DateTimeFormat($i18n.locale, {
          dateStyle: 'medium',
          timeStyle: 'short'
        }).format(item.readDate)
      }}
    </template>

    <template v-slot:footer.prepend>
      <v-btn icon @click="loadData">
        <v-icon>mdi-refresh</v-icon>
      </v-btn>
    </template>
  </v-data-table>
</template>

<script lang="ts">
import Vue from 'vue'
import {ERROR} from '@/types/events'
import {RecentReadDto} from '@/types/komga-users'

export default Vue.extend({
  name: 'RecentReadsTable',
  data: function () {
    return {
      items: [] as RecentReadDto[],
      loading: true,
    }
  },
  computed: {
    headers(): object[] {
      return [
        {text: this.$t('recent_reads.user').toString(), value: 'userEmail'},
        {text: this.$t('recent_reads.book').toString(), value: 'bookTitle'},
        {text: this.$t('recent_reads.series').toString(), value: 'seriesTitle'},
        {text: this.$t('recent_reads.progress').toString(), value: 'progress', sortable: false},
        {text: this.$t('recent_reads.read_date').toString(), value: 'readDate'},
      ]
    },
  },
  mounted() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        this.items = await this.$komgaUsers.getRecentReads(10)
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
      this.loading = false
    },
  },
})
</script>
