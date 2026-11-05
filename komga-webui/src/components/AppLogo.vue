<template>
  <v-img
    :src="src"
    :max-width="maxWidth"
    contain
  />
</template>

<script lang="ts">
import Vue from 'vue'
import defaultLogo from '../assets/logo.svg'
import {logoUrl} from '@/functions/branding'

export default Vue.extend({
  name: 'AppLogo',
  props: {
    maxWidth: {
      type: Number,
      required: false,
      default: undefined,
    },
  },
  computed: {
    src(): string {
      return this.$store.getters.logoAvailable
        ? logoUrl(this.$store.getters.logoVersion)
        : defaultLogo
    },
  },
})
</script>
