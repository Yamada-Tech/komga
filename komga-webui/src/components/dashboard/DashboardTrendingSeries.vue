<template>
  <v-container fluid v-if="seriesList.length > 0">
    <div class="d-flex align-center mb-3">
      <v-icon color="orange" class="mr-2">mdi-fire</v-icon>
      <div class="title font-weight-bold">みんなが読んでるマンガ</div>
      <v-spacer/>
      <span class="caption text--secondary">直近1週間のアクティブ作品</span>
    </div>

    <v-slide-group show-arrows>
      <v-slide-item
        v-for="series in seriesList"
        :key="series.seriesId"
      >
        <v-card
          :to="series.to"
          width="150"
          class="ma-2"
          :ripple="false"
        >
          <v-img
            :src="seriesThumbnailUrl(series.seriesId)"
            aspect-ratio="0.7071"
            contain
            class="grey lighten-2"
          />
          <v-card-subtitle class="px-2 pt-2 pb-1 text-truncate">{{ series.title }}</v-card-subtitle>
          <v-card-text class="px-2 pt-0 pb-2 caption text--secondary d-flex align-center">
            <v-icon size="16" class="mr-1">mdi-account-group</v-icon>
            <span>{{ series.uniqueReaders }} 人が閲覧中</span>
          </v-card-text>
        </v-card>
      </v-slide-item>
    </v-slide-group>
  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import {seriesThumbnailUrl} from '@/functions/urls'
import {RawLocation} from 'vue-router'
import {TopSeriesReadingStatAggregateDto} from '@/types/komga-reading-stats'
import {SeriesDto} from '@/types/komga-series'

interface TrendingSeries {
  seriesId: string,
  uniqueReaders: number,
  title: string,
  to: RawLocation,
}

export default Vue.extend({
  name: 'DashboardTrendingSeries',
  data: () => {
    return {
      seriesList: [] as TrendingSeries[],
    }
  },
  async mounted() {
    await this.loadTrendingSeries()
  },
  methods: {
    seriesThumbnailUrl,
    async loadTrendingSeries() {
      try {
        const ranked = await this.$komgaReadingStats.getTopSeriesByPeriod('weekly', 12)
        const cards =
          await Promise.all(
            ranked.map(async (it: TopSeriesReadingStatAggregateDto) => {
              try {
                const series = await this.$komgaSeries.getOneSeries(it.seriesId)
                return this.toTrendingSeriesCard(series, it)
              } catch (e) {
                return undefined
              }
            }),
          )
        this.seriesList = cards.filter((it): it is TrendingSeries => it !== undefined)
      } catch (e) {
        this.$warn('Unable to load trending series data for dashboard. This can happen due to network, permission, or temporary API availability issues.', e)
      }
    },
    toTrendingSeriesCard(series: SeriesDto, stats: TopSeriesReadingStatAggregateDto): TrendingSeries {
      return {
        seriesId: series.id,
        uniqueReaders: stats.uniqueReaders,
        title: series.metadata.title || series.name,
        to: {
          name: series.oneshot ? 'browse-oneshot' : 'browse-series',
          params: {seriesId: series.id},
        },
      }
    },
  },
})
</script>
