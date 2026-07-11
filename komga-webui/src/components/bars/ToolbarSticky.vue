<template>
  <v-toolbar flat
             :color="color"
             class="sticky-bar"
             :style="barStyle"
             :elevation="elevation"
             dense
             height="42"
  >
    <slot/>
  </v-toolbar>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'ToolbarSticky',
  computed: {
    barStyle (): any {
      if (['xs', 'sm'].includes(this.$vuetify.breakpoint.name.toString())) {
        return { 'top': '48px' }
      } else {
        return { 'top': '48px' }
      }
    },
  },
  props: {
    elevation: {
      type: Number,
      default: undefined,
    },
    color: {
      type: String,
      default: 'contrast-1',
    },
  },
})
</script>

<style scoped>
.sticky-bar {
  position: -webkit-sticky;
  position: sticky;
  z-index: 2
}

.sticky-bar :deep(.v-btn--icon) {
  width: 30px;
  height: 30px;
}

.sticky-bar :deep(.v-btn) {
  min-width: 30px;
}

.sticky-bar :deep(.v-icon) {
  font-size: 18px;
}

.sticky-bar :deep(.v-input.v-text-field--outlined .v-input__slot) {
  min-height: 32px;
}

.sticky-bar :deep(.v-select__selection) {
  font-size: 0.82rem;
}
</style>
