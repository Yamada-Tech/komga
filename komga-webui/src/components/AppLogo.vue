<template>
  <v-img
    :src="logoSrc"
    v-bind="$attrs"
    @error="onError"
  />
</template>

<script lang="ts">
import Vue from 'vue'

const DEFAULT_LOGO = require('../assets/logo.svg')
const CUSTOM_LOGO_URL = '/api/v1/logo'

export default Vue.extend({
  name: 'AppLogo',
  inheritAttrs: false,
  data: () => ({
    hasError: false,
  }),
  computed: {
    logoSrc(): string {
      return this.hasError ? DEFAULT_LOGO : CUSTOM_LOGO_URL
    },
  },
  methods: {
    onError() {
      this.hasError = true
    },
  },
})
</script>
