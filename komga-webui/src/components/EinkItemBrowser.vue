<template>
  <div class="eink-browser" :class="`eink-browser--${deviceProfile}`">
    <div v-if="hasItems" :class="gridClass" :style="gridStyle">
      <div v-for="item in currentPageItems"
           :key="item.id"
           class="eink-grid-cell"
      >
        <item-card
          :item="item"
          :item-context="itemContext"
          :width="itemWidth"
        ></item-card>
      </div>
    </div>

    <v-row v-else justify="center">
      <slot name="empty"></slot>
    </v-row>

    <div v-if="totalPages > 1" class="eink-pagination">
      <v-btn
        x-large
        :disabled="currentPage <= 1"
        @click="prevPage"
        class="eink-nav-btn"
      >
        {{ $t('common.previous_page') }}
      </v-btn>
      <span class="eink-page-info">{{ currentPage }} / {{ totalPages }}</span>
      <v-btn
        x-large
        :disabled="currentPage >= totalPages"
        @click="nextPage"
        class="eink-nav-btn"
      >
        {{ $t('common.next_page') }}
      </v-btn>
    </div>
  </div>
</template>

<script lang="ts">
import ItemCard from '@/components/ItemCard.vue'
import Vue from 'vue'
import {ItemContext} from '@/types/items'

export default Vue.extend({
  name: 'EinkItemBrowser',
  components: {ItemCard},
  props: {
    items: {
      type: Array,
      required: true,
    },
    itemContext: {
      type: Array as () => ItemContext[],
      default: () => [],
    },
  },
  data: function () {
    return {
      currentPage: 1,
    }
  },
  watch: {
    items() {
      this.currentPage = 1
    },
  },
  computed: {
    isPortrait(): boolean {
      return this.$vuetify.breakpoint.height >= this.$vuetify.breakpoint.width
    },
    shortSide(): number {
      return Math.min(this.$vuetify.breakpoint.width, this.$vuetify.breakpoint.height)
    },
    longSide(): number {
      return Math.max(this.$vuetify.breakpoint.width, this.$vuetify.breakpoint.height)
    },
    aspectRatio(): number {
      return this.longSide / this.shortSide
    },
    deviceProfile(): string {
      if (this.isPortrait && this.aspectRatio >= 1.9 && this.shortSide <= 450) return 'palma'
      if (this.shortSide <= 430) return 'inch6'
      if (this.shortSide <= 540) return 'inch68'
      return 'inch78'
    },
    columns(): number {
      switch (this.deviceProfile) {
        case 'palma':
          return this.isPortrait ? 2 : 3
        case 'inch6':
          return this.isPortrait ? 2 : 3
        case 'inch68':
          return this.isPortrait ? 2 : 4
        case 'inch78':
        default:
          return this.isPortrait ? 3 : 5
      }
    },
    rows(): number {
      switch (this.deviceProfile) {
        case 'palma':
          return 2
        case 'inch6':
          return 2
        case 'inch68':
          return this.isPortrait ? 3 : 2
        case 'inch78':
        default:
          return this.isPortrait ? 3 : 2
      }
    },
    itemsPerPage(): number {
      return this.columns * this.rows
    },
    gridClass(): string {
      return 'eink-grid'
    },
    gridStyle(): Record<string, string> {
      return {
        gridTemplateColumns: `repeat(${this.columns}, minmax(0, 1fr))`,
        gridTemplateRows: `repeat(${this.rows}, minmax(0, 1fr))`,
      }
    },
    totalPages(): number {
      if (!this.items.length) return 0
      return Math.ceil(this.items.length / this.itemsPerPage)
    },
    hasItems(): boolean {
      return this.items.length > 0
    },
    currentPageItems(): any[] {
      const start = (this.currentPage - 1) * this.itemsPerPage
      return (this.items as any[]).slice(start, start + this.itemsPerPage)
    },
    itemWidth(): number {
      const horizontalPadding = this.deviceProfile === 'palma' ? 24 : 32
      const availableWidth = this.$vuetify.breakpoint.width - horizontalPadding
      return Math.max(84, Math.floor(availableWidth / this.columns) - 10)
    },
  },
  methods: {
    prevPage() {
      if (this.currentPage > 1) this.currentPage--
    },
    nextPage() {
      if (this.currentPage < this.totalPages) this.currentPage++
    },
  },
})
</script>

<style scoped>
.eink-browser {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.eink-grid {
  display: grid;
  flex: 1;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
}

.eink-grid-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}

.eink-grid-cell :deep(.v-card) {
  margin: auto;
}

.eink-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  border-top: 2px solid #000000;
  background-color: #FFFFFF;
  flex-shrink: 0;
}

.eink-nav-btn {
  min-width: 120px;
  font-size: 1.1rem;
  font-weight: bold;
}

.eink-page-info {
  font-size: 1.2rem;
  font-weight: bold;
  color: #000000;
}

.eink-browser--palma .eink-grid {
  gap: 4px;
  padding: 4px;
}

.eink-browser--palma .eink-nav-btn {
  min-width: 92px;
  font-size: 0.95rem;
}

.eink-browser--palma .eink-page-info {
  font-size: 1rem;
}

.eink-browser--inch6 .eink-nav-btn {
  min-width: 104px;
  font-size: 1rem;
}

.eink-browser--inch68 .eink-nav-btn,
.eink-browser--inch78 .eink-nav-btn {
  min-width: 120px;
}
</style>
