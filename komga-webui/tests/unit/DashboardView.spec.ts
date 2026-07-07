import {RecommendedViewSection} from '@/types/komga-clientsettings'

jest.mock('@/i18n', () => ({
  t: (key: string) => key,
}))

const DashboardView = require('@/views/DashboardView.vue').default

describe('DashboardView E-Ink section switching', () => {
  const methods = (DashboardView as any).options.methods
  const computed = (DashboardView as any).options.computed

  it('shows only active section in E-Ink mode', () => {
    const context = {
      einkMode: true,
      activeSection: RecommendedViewSection.RECENTLY_UPDATED_SERIES,
    }

    expect(methods.isSectionVisible.call(context, {value: RecommendedViewSection.RECENTLY_UPDATED_SERIES})).toBe(true)
    expect(methods.isSectionVisible.call(context, {value: RecommendedViewSection.RECENTLY_ADDED_SERIES})).toBe(false)
  })

  it('switches visible section when activeSection changes', () => {
    const context = {
      einkMode: true,
      activeSection: RecommendedViewSection.RECENTLY_ADDED_SERIES,
    }

    expect(methods.isSectionVisible.call(context, {value: RecommendedViewSection.RECENTLY_ADDED_SERIES})).toBe(true)
    context.activeSection = RecommendedViewSection.RECENTLY_UPDATED_SERIES
    expect(methods.isSectionVisible.call(context, {value: RecommendedViewSection.RECENTLY_UPDATED_SERIES})).toBe(true)
    expect(methods.isSectionVisible.call(context, {value: RecommendedViewSection.RECENTLY_ADDED_SERIES})).toBe(false)
  })

  it('keeps trending visible only when trending tab is active in E-Ink mode', () => {
    const context = {
      einkMode: true,
      activeSection: RecommendedViewSection.TRENDING,
      trendingEnabled: true,
    }

    expect(methods.isTrendingVisible.call(context)).toBe(true)
    context.activeSection = RecommendedViewSection.RECENTLY_UPDATED_SERIES
    expect(methods.isTrendingVisible.call(context)).toBe(false)
  })

  it('returns fixed E-Ink tabs for trending, recently updated and recently added', () => {
    const labels: Record<string, string> = {
      'dashboard.trending': 'トレンド',
      'dashboard.recently_updated_series': '最近更新',
      'dashboard.recently_added_series': '新着追加',
    }
    const context = {
      $t: (key: string) => labels[key] ?? key,
    }

    const tabs = computed.einkSectionTabs.call(context)
    expect(tabs.map((it: any) => it.value)).toEqual([
      RecommendedViewSection.TRENDING,
      RecommendedViewSection.RECENTLY_UPDATED_SERIES,
      RecommendedViewSection.RECENTLY_ADDED_SERIES,
    ])
  })
})
