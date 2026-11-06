import ServerService from '@/services/server.service'
import { AxiosInstance } from 'axios'
import _Vue from 'vue'

export default {
  install (
    Vue: typeof _Vue,
    { http }: { http: AxiosInstance }) {
    Vue.prototype.$komgaServer = new ServerService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $komgaServer: ServerService;
  }
}
