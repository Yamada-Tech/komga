import {AxiosInstance} from 'axios'
import _Vue from 'vue'
import KomgaLogoService from '@/services/komga-logo.service'

export default {
  install(Vue: typeof _Vue, {http}: { http: AxiosInstance }) {
    Vue.prototype.$komgaLogo = new KomgaLogoService(http)
  },
}

declare module 'vue/types/vue' {
  interface Vue {
    $komgaLogo: KomgaLogoService;
  }
}
