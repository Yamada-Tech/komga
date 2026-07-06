<template>
  <horizontal-scroller
    v-if="seriesList.length > 0"
    class="mb-4"
  >
    <template v-slot:prepend>
      <div class="title">{{ $t('dashboard.trending') }}</div>
    </template>
    <template v-slot:content>
      <div
        v-for="item in seriesList"
        :key="item.series.id"
        class="my-2 mx-2"
      >
        <item-card
          :item="item.series"
          :width="cardWidth"
          :action-menu="false"
        />
        <div
          class="caption text--secondary text-truncate"
          :style="`width: ${cardWidth}px`"
          :aria-label="readerCountLabel(item.uniqueReaders)"
        >
          {{ $t('dashboard.trending_count', { count: item.uniqueReaders }) }}
        </div>
      </div>
    </template>
  </horizontal-scroller>
</template>

<script lang="ts">
import Vue from 'vue'
import {TopSeriesReadingStatAggregateDto} from '@/types/komga-reading-stats'
import {SeriesDto} from '@/types/komga-series'
import HorizontalScroller from '@/components/HorizontalScroller.vue'
import ItemCard from '@/components/ItemCard.vue'

interface TrendingSeries {
  series: SeriesDto,
  uniqueReaders: number,
}

export default Vue.extend({
  name: 'DashboardTrendingSeries',
  components: {HorizontalScroller, ItemCard},
  data: () => {
    return {
      period: 'weekly',
      seriesList: [] as TrendingSeries[],
    }
  },
  async mounted() {
    await this.loadTrendingSeries()
  },
  computed: {
    cardWidth(): number {
      return this.$vuetify.breakpoint.xs ? 120 : 150
    },
  },
  methods: {
    readerCountLabel(count: number): string {
      return this.$t('dashboard.trending_count', { count }).toString()
    },
    async loadTrendingSeries() {
      try {
        const ranked = await this.$komgaReadingStats.getTopSeriesByPeriod(this.period, 12)
        const cards =
          await Promise.all(
            ranked.map(async (it: TopSeriesReadingStatAggregateDto) => {
              try {
                const series = await this.$komgaSeries.getOneSeries(it.seriesId)
                return {series, uniqueReaders: it.uniqueReaders} as TrendingSeries
              } catch (e) {
                return undefined
              }
            }),
          )
        this.seriesList = cards.filter((it): it is TrendingSeries => it !== undefined)
      } catch (e) {
        this.$warn('Unable to load trending series for dashboard', e)
      }
    },
  },
})
</script>
