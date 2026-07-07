<template>
  <div class="eink-browser">
    <div v-if="hasItems" :class="gridClass">
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
    itemsPerPage(): number {
      return this.isPortrait ? 6 : 4
    },
    columns(): number {
      return this.isPortrait ? 2 : 4
    },
    gridClass(): string {
      return `eink-grid eink-grid-cols-${this.columns}`
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
      const availableWidth = this.$vuetify.breakpoint.width - 32
      return Math.floor(availableWidth / this.columns) - 8
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

.eink-grid-cols-2 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(3, 1fr);
}

.eink-grid-cols-4 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(1, 1fr);
}

.eink-grid-cell {
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
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
</style>
