<template>
  <div class="eink-browser" :class="isCompact ? 'eink-browser--compact' : ''">
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
      <v-pagination
        v-model="currentPage"
        :length="totalPages"
        :total-visible="paginationVisible"
      />
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
    reservedHeight: {
      type: Number,
      default: 0,
    },
  },
  data: function () {
    return {
      currentPage: 1,
      viewportWidth: 0,
      viewportHeight: 0,
    }
  },
  watch: {
    items() {
      this.currentPage = 1
    },
  },
  mounted() {
    this.syncViewportSize()
    window.addEventListener('resize', this.syncViewportSize)
    if (window.visualViewport) {
      window.visualViewport.addEventListener('resize', this.syncViewportSize)
      window.visualViewport.addEventListener('scroll', this.syncViewportSize)
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewportSize)
    if (window.visualViewport) {
      window.visualViewport.removeEventListener('resize', this.syncViewportSize)
      window.visualViewport.removeEventListener('scroll', this.syncViewportSize)
    }
  },
  computed: {
    isPortrait(): boolean {
      return this.effectiveHeight >= this.effectiveWidth
    },
    effectiveWidth(): number {
      return this.viewportWidth || this.$vuetify.breakpoint.width
    },
    effectiveHeight(): number {
      return this.viewportHeight || this.$vuetify.breakpoint.height
    },
    isCompact(): boolean {
      const width = this.effectiveWidth
      const height = this.effectiveHeight
      return Math.min(width, height) <= 430 || (width * height) <= 320000
    },
    availableWidth(): number {
      const horizontalPadding = this.isCompact ? 24 : 32
      return Math.max(200, this.effectiveWidth - horizontalPadding)
    },
    availableHeight(): number {
      const paginationHeight = 68
      const gridPadding = this.isCompact ? 16 : 24
      const shortSide = Math.min(this.effectiveWidth, this.effectiveHeight)
      const viewportMargin = shortSide <= 430 ? 44 : shortSide <= 720 ? 56 : 68
      return Math.max(120, this.effectiveHeight - viewportMargin - this.reservedHeight - paginationHeight - gridPadding)
    },
    cardMetaHeight(): number {
      return this.isCompact ? 42 : 58
    },
    columns(): number {
      const minCardWidth = this.isCompact ? 120 : (this.isPortrait ? 165 : 190)
      const maxColumns = !this.isCompact && !this.isPortrait ? 4 : 5
      const minColumns = this.isPortrait ? 2 : 3
      return Math.max(minColumns, Math.min(maxColumns, Math.floor(this.availableWidth / minCardWidth)))
    },
    rows(): number {
      const estimatedCardHeight = Math.round((this.itemWidth / 0.7071) + this.cardMetaHeight)
      const minRows = 1
      return Math.max(minRows, Math.min(6, Math.floor(this.availableHeight / estimatedCardHeight)))
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
      const widthByColumns = Math.floor(this.availableWidth / this.columns) - 10
      const targetRowsForSizing = 1
      const maxWidthForTargetRows = Math.floor(((this.availableHeight / targetRowsForSizing) - this.cardMetaHeight) * 0.7071)
      const floorWidth = this.isCompact ? 40 : 56
      return Math.max(floorWidth, Math.min(widthByColumns, Math.max(floorWidth, maxWidthForTargetRows)))
    },
    paginationVisible(): number {
      if (this.effectiveWidth <= 420) return 5
      if (this.effectiveWidth <= 768) return 7
      return 11
    },
  },
  methods: {
    syncViewportSize() {
      const vv = window.visualViewport
      this.viewportWidth = Math.round(vv?.width || window.innerWidth || this.$vuetify.breakpoint.width)
      this.viewportHeight = Math.round(vv?.height || window.innerHeight || this.$vuetify.breakpoint.height)
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
  justify-content: center;
  align-items: center;
  padding: 8px 16px;
  border-top: 2px solid #000000;
  background-color: #FFFFFF;
  flex-shrink: 0;
}

.eink-browser--compact .eink-grid {
  gap: 4px;
  padding: 4px;
}

</style>
