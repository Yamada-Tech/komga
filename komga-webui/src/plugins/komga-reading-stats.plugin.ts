import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import KomgaReadingStatsService from '@/services/komga-reading-stats.service'

export default {
  install(
    Vue: typeof _Vue,
    {http}: { http: AxiosInstance }) {
    Vue.prototype.$komgaReadingStats = new KomgaReadingStatsService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $komgaReadingStats: KomgaReadingStatsService;
  }
}
