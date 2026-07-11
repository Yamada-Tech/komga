<template>
  <div :style="$vuetify.breakpoint.xs ? 'margin-bottom: 56px' : undefined">
    <toolbar-sticky v-if="individualLibrary && selectedSeries.length === 0 && selectedBooks.length === 0">
      <!--   Action menu   -->
      <library-actions-menu v-if="isAdmin && library"
                            :library="library"/>

      <v-toolbar-title>
        <span>{{ library ? library.name : $t('common.all_libraries') }}</span>
      </v-toolbar-title>

      <v-spacer/>

      <library-navigation v-if="individualLibrary && $vuetify.breakpoint.mdAndUp" :libraryId="libraryId"/>

      <v-spacer/>

    </toolbar-sticky>

    <library-navigation v-if="individualLibrary && $vuetify.breakpoint.smAndDown && !einkMode" :libraryId="libraryId"
                        bottom-navigation/>

    <multi-select-bar
      v-model="selectedSeries"
      kind="series"
      @unselect-all="selectedSeries = []"
      @mark-read="markSelectedSeriesRead"
      @mark-unread="markSelectedSeriesUnread"
      @add-to-collection="addToCollection"
      @add-to-readlist="addSeriesBooksToReadList"
      @edit="editMultipleSeries"
      @delete="deleteSeries"
    />

    <multi-select-bar
      v-model="selectedBooks"
      kind="books"
      :oneshots="selectedOneshots"
      @unselect-all="selectedBooks = []"
      @mark-read="markSelectedBooksRead"
      @mark-unread="markSelectedBooksUnread"
      @add-to-readlist="addToReadList"
      @add-to-collection="addOneshotsToCollection"
      @edit="editMultipleBooks"
      @bulk-edit="bulkEditMultipleBooks"
      @delete="deleteBooks"
    />

    <v-container fluid :class="einkMode ? 'eink-dashboard-container eink-content-max' : ''">
      <div v-if="einkMode" class="eink-selector-wrap">
        <v-select
          v-model="activeSection"
          :items="einkSectionTabs"
          item-text="label"
          item-value="value"
          dense
          outlined
          hide-details
          class="eink-section-selector"
        />
      </div>

      <empty-state v-if="allEmpty && !loading"
                   :title="$t('common.nothing_to_show')"
                   icon="mdi-help-circle"
                   icon-color="secondary"
      >
      </empty-state>

      <template v-for="(section, i) in sections">
        <dashboard-trending-series
          v-bind:key="`trend-${i}`"
          v-if="!einkMode && section.value === RecommendedViewSection.TRENDING && trendingSeries.length > 0 && isSectionVisible(section)"
          class="mb-4"
        />

        <div
          v-bind:key="`eink-trend-${i}`"
          v-if="einkMode && section.value === RecommendedViewSection.TRENDING && trendingSeries.length > 0 && isSectionVisible(section)"
          class="eink-section-block"
        >
          <eink-item-browser
            :items="trendingSeries"
            :reserved-height="einkReservedHeight"
          />
        </div>

        <div
          v-bind:key="`eink-sec-${i}`"
          v-if="einkMode && section.value !== RecommendedViewSection.TRENDING && section.loader && section.loader.items.length !== 0 && isSectionVisible(section)"
          class="eink-section-block"
        >
          <eink-item-browser
            :items="section.loader.items"
            :item-context="section.itemContext"
            :reserved-height="einkReservedHeight"
          />
        </div>

        <horizontal-scroller
          v-bind:key="i"
          v-if="!einkMode && section.value !== RecommendedViewSection.TRENDING && section.loader && section.loader.items.length !== 0"
          v-show="isSectionVisible(section)"
          class="mb-4"
          :tick="section.loader.tick"
          @scroll-changed="(percent) => scrollChanged(section.loader, percent)"
        >
          <template v-slot:prepend>
            <div class="title">{{ $t(`dashboard.${section.value.toLowerCase()}`) }}</div>
          </template>
          <template v-slot:content>
            <item-browser v-if="section.type ===SectionType.BOOK"
                          :items="section.loader.items"
                          :item-context="section.itemContext"
                          nowrap
                          :edit-function="isAdmin ? singleEditBook : undefined"
                          :selected.sync="selectedBooks"
                          :selectable="selectedSeries.length === 0"
                          :fixed-item-width="fixedCardWidth"
            />
            <item-browser v-if="section.type === SectionType.SERIES"
                          :items="section.loader.items"
                          :item-context="section.itemContext"
                          nowrap
                          :edit-function="isAdmin ? singleEditSeries : undefined"
                          :selected.sync="selectedSeries"
                          :selectable="selectedBooks.length === 0"
                          :fixed-item-width="fixedCardWidth"
            />
          </template>
        </horizontal-scroller>
      </template>

      <v-fab-transition>
        <v-btn
          v-if="!einkMode"
          fab
          bottom
          right
          fixed
          :class="individualLibrary && $vuetify.breakpoint.smAndDown ? 'mb-12' : undefined"
          @click="modalEditRecommended = true"
        >
          <v-icon>mdi-pencil</v-icon>
        </v-btn>
      </v-fab-transition>

      <edit-recommended-dialog
        v-model="modalEditRecommended"
        :view-config.sync="viewConfig"
        @reset-defaults="resetDefaultView"
      />
    </v-container>
  </div>
</template>

<script lang="ts">
import MultiSelectBar from '@/components/bars/MultiSelectBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import HorizontalScroller from '@/components/HorizontalScroller.vue'
import ItemBrowser from '@/components/ItemBrowser.vue'
import EinkItemBrowser from '@/components/EinkItemBrowser.vue'
import ToolbarSticky from '@/components/bars/ToolbarSticky.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import LibraryNavigation from '@/components/LibraryNavigation.vue'
import {ReadStatus} from '@/types/enum-books'
import {BookDto} from '@/types/komga-books'
import {
  BOOK_ADDED,
  BOOK_CHANGED,
  BOOK_DELETED,
  READPROGRESS_CHANGED,
  READPROGRESS_DELETED,
  READPROGRESS_SERIES_CHANGED,
  READPROGRESS_SERIES_DELETED,
  SERIES_ADDED,
  SERIES_CHANGED,
  SERIES_DELETED,
} from '@/types/events'
import Vue from 'vue'
import {Oneshot, SeriesDto} from '@/types/komga-series'
import {LIBRARIES_ALL, LIBRARY_ROUTE} from '@/types/library'
import {throttle} from 'lodash'
import {subMonths} from 'date-fns'
import {BookSseDto, ReadProgressSeriesSseDto, ReadProgressSseDto, SeriesSseDto} from '@/types/komga-sse'
import {LibraryDto} from '@/types/komga-libraries'
import {PageLoader} from '@/types/pageLoader'
import {ItemContext} from '@/types/items'
import {
  BookSearch,
  SearchConditionAllOfBook,
  SearchConditionAnyOfBook,
  SearchConditionAnyOfSeries,
  SearchConditionBook,
  SearchConditionLibraryId,
  SearchConditionReadStatus,
  SearchConditionReleaseDate,
  SearchConditionSeriesId,
  SearchOperatorAfter,
  SearchOperatorIs,
} from '@/types/komga-search'
import {
  CLIENT_SETTING,
  ClientSettingsRecommendedView,
  ClientSettingsRecommendedViewSection,
  ClientSettingUserUpdateDto,
  RECOMMENDED_DEFAULT,
  RecommendedViewSection,
} from '@/types/komga-clientsettings'
import EditRecommendedDialog from '@/components/dialogs/EditRecommendedDialog.vue'
import DashboardTrendingSeries from '@/components/dashboard/DashboardTrendingSeries.vue'
import {Theme} from '@/types/themes'
import {TopSeriesReadingStatAggregateDto} from '@/types/komga-reading-stats'

interface SectionConfig {
  loader: PageLoader<any> | undefined,
  type: SectionType,
  value: RecommendedViewSection,
  itemContext?: ItemContext[] | undefined,
}

enum SectionType {
  SERIES,
  BOOK,
}

export default Vue.extend({
  name: 'DashboardView',
  components: {
    DashboardTrendingSeries,
    EditRecommendedDialog,
    HorizontalScroller,
    EinkItemBrowser,
    EmptyState,
    ToolbarSticky,
    LibraryNavigation,
    ItemBrowser,
    MultiSelectBar,
    LibraryActionsMenu,
  },
  data: () => {
    return {
      ItemContext,
      SectionType,
      RecommendedViewSection,
      loading: false,
      modalEditRecommended: false,
      loaderRecentlyAddedSeries: undefined as PageLoader<SeriesDto> | undefined,
      loaderRecentlyUpdatedSeries: undefined as PageLoader<SeriesDto> | undefined,
      loaderRecentlyAddedBooks: undefined as PageLoader<BookDto> | undefined,
      loaderKeepReadingBooks: undefined as PageLoader<BookDto> | undefined,
      loaderOnDeckBooks: undefined as PageLoader<BookDto> | undefined,
      loaderRecentlyReleasedBooks: undefined as PageLoader<BookDto> | undefined,
      loaderRecentlyReadBooks: undefined as PageLoader<BookDto> | undefined,
      trendingSeries: [] as SeriesDto[],
      selectedSeries: [] as SeriesDto[],
      selectedBooks: [] as BookDto[],
      activeSection: RecommendedViewSection.TRENDING as RecommendedViewSection,
    }
  },
  created() {
    this.$eventHub.$on(SERIES_ADDED, this.seriesChanged)
    this.$eventHub.$on(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$on(SERIES_DELETED, this.seriesChanged)
    this.$eventHub.$on(BOOK_ADDED, this.bookChanged)
    this.$eventHub.$on(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$on(BOOK_DELETED, this.bookChanged)
    this.$eventHub.$on(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$on(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$on(READPROGRESS_SERIES_CHANGED, this.readProgressSeriesChanged)
    this.$eventHub.$on(READPROGRESS_SERIES_DELETED, this.readProgressSeriesChanged)
  },
  beforeDestroy() {
    this.$eventHub.$off(SERIES_ADDED, this.seriesChanged)
    this.$eventHub.$off(SERIES_CHANGED, this.seriesChanged)
    this.$eventHub.$off(SERIES_DELETED, this.seriesChanged)
    this.$eventHub.$off(BOOK_ADDED, this.bookChanged)
    this.$eventHub.$off(BOOK_CHANGED, this.bookChanged)
    this.$eventHub.$off(BOOK_DELETED, this.bookChanged)
    this.$eventHub.$off(READPROGRESS_CHANGED, this.readProgressChanged)
    this.$eventHub.$off(READPROGRESS_DELETED, this.readProgressChanged)
    this.$eventHub.$off(READPROGRESS_SERIES_CHANGED, this.readProgressSeriesChanged)
    this.$eventHub.$off(READPROGRESS_SERIES_DELETED, this.readProgressSeriesChanged)
  },
  mounted() {
    if (this.individualLibrary) this.$store.commit('setLibraryRoute', {
      id: this.libraryId,
      route: LIBRARY_ROUTE.RECOMMENDED,
    })
    this.setupLoaders(this.libraryId)
    this.syncEinkActiveSection()
    this.loadAll()
  },
  props: {
    libraryId: {
      type: String,
      default: LIBRARIES_ALL,
    },
  },
  watch: {
    libraryIds() {
      this.setupLoaders(this.libraryId)
      this.loadAll()
      this.syncEinkActiveSection()
    },
    einkMode() {
      this.syncEinkActiveSection()
    },
    '$store.state.komgaLibraries.libraries': {
      handler(val) {
        if (val.length === 0) this.$router.push({name: 'welcome'})
        else this.reload()
      },
    },
    '$store.getters.getLibrariesPinned': {
      handler(val) {
        if (val.length === 0) this.$router.push({name: 'no-pins'})
      },
    },
  },
  computed: {
    settingsKey(): string {
      return `${CLIENT_SETTING.WEBUI_RECOMMENDED}.${this.libraryId}`.toLowerCase()
    },
    viewConfig: {
      get: function (): ClientSettingsRecommendedView {
        try {
          return JSON.parse(this.$store.getters.getClientSettings[this.settingsKey].value) as ClientSettingsRecommendedView
        } catch (_) {
        }
        return RECOMMENDED_DEFAULT
      },
      set: async function (view: ClientSettingsRecommendedView) {
        let newSettings = {} as Record<string, ClientSettingUserUpdateDto>
        newSettings[this.settingsKey] = {
          value: JSON.stringify(view),
        }
        await this.$komgaSettings.updateClientSettingUser(newSettings)
        await this.$store.dispatch('getClientSettingsUser')
        this.setupLoaders(this.libraryId)
        this.loadAll(true)
      },
    },
    sections(): SectionConfig[] {
      const sections = [] as SectionConfig[]
      this.recommendedSections.forEach((it: ClientSettingsRecommendedViewSection) => {
        const config = this.getSectionConfig(it.section)
        if (config != undefined) sections.push(config)
      })
      return sections
    },
    recommendedSections(): ClientSettingsRecommendedViewSection[] {
      return this.viewConfig.sections && this.viewConfig.sections.length > 0
        ? this.viewConfig.sections
        : RECOMMENDED_DEFAULT.sections
    },
    library(): LibraryDto | undefined {
      return this.getLibraryLazy(this.libraryId)
    },
    libraryIds(): string[] {
      return this.libraryId !== LIBRARIES_ALL ? [this.libraryId] : this.$store.getters.getLibrariesPinned.map((it: LibraryDto) => it.id)
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    fixedCardWidth(): number {
      return this.$vuetify.breakpoint.xs ? 120 : 150
    },
    allEmpty(): boolean {
      const trendingEmpty = this.trendingSeries.length === 0
      return (this.loaderRecentlyAddedSeries == undefined || this.loaderRecentlyAddedSeries?.items.length === 0) &&
        (this.loaderRecentlyUpdatedSeries == undefined || this.loaderRecentlyUpdatedSeries?.items.length === 0) &&
        (this.loaderRecentlyAddedBooks == undefined || this.loaderRecentlyAddedBooks?.items.length === 0) &&
        (this.loaderKeepReadingBooks == undefined || this.loaderKeepReadingBooks?.items.length === 0) &&
        (this.loaderOnDeckBooks == undefined || this.loaderOnDeckBooks?.items.length === 0) &&
        (this.loaderRecentlyReleasedBooks == undefined || this.loaderRecentlyReleasedBooks?.items.length === 0) &&
        (this.loaderRecentlyReadBooks == undefined || this.loaderRecentlyReadBooks?.items.length === 0) &&
        trendingEmpty
    },
    individualLibrary(): boolean {
      return this.libraryId !== LIBRARIES_ALL
    },
    selectedOneshots(): boolean {
      return this.selectedBooks.every(b => b.oneshot)
    },
    trendingEnabled(): boolean {
      return this.hasSection(RecommendedViewSection.TRENDING)
    },
    einkMode(): boolean {
      return this.$store.state.persistedState.theme === Theme.EINK
    },
    einkShortSide(): number {
      return Math.min(this.$vuetify.breakpoint.width, this.$vuetify.breakpoint.height)
    },
    einkLongSide(): number {
      return Math.max(this.$vuetify.breakpoint.width, this.$vuetify.breakpoint.height)
    },
    einkAspectRatio(): number {
      return this.einkLongSide / this.einkShortSide
    },
    einkDeviceProfile(): string {
      if (this.einkAspectRatio >= 1.9 && this.einkShortSide <= 450) return 'palma'
      if (this.einkShortSide <= 430) return 'inch6'
      if (this.einkShortSide <= 540) return 'inch68'
      return 'inch78'
    },
    einkSectionTabs(): { value: RecommendedViewSection, label: string }[] {
      return this.recommendedSections.map(it => ({
        value: it.section,
        label: this.$t(`dashboard.${it.section.toLowerCase()}`).toString(),
      }))
    },
    einkReservedHeight(): number {
      if (this.einkDeviceProfile === 'palma') return 185
      if (this.einkDeviceProfile === 'inch6') return 175
      if (this.einkDeviceProfile === 'inch68') return 165
      return 155
    },
  },
  methods: {
    hasEinkSectionContent(section: RecommendedViewSection): boolean {
      if (section === RecommendedViewSection.TRENDING) return this.trendingSeries.length > 0
      const config = this.getSectionConfig(section)
      return !!config?.loader && config.loader.items.length > 0
    },
    async resetDefaultView() {
      await this.$komgaSettings.deleteClientSettingUser([this.settingsKey])
      await this.$store.dispatch('getClientSettingsUser')
      this.setupLoaders(this.libraryId)
      this.loadAll(true)
    },
    hasSection(section: RecommendedViewSection): boolean {
      return this.recommendedSections.some(it => it.section === section)
    },
    isSectionVisible(section: SectionConfig): boolean {
      return !this.einkMode || this.activeSection === section.value
    },
    syncEinkActiveSection() {
      if (!this.einkMode) return
      const availableSections = this.einkSectionTabs
        .map(it => it.value)
        .filter(it => this.hasSection(it))
      if (availableSections.length === 0) {
        return
      }
      if (!availableSections.includes(this.activeSection)) {
        this.activeSection = availableSections[0]
      }
    },
    syncEinkSectionWithContent() {
      if (!this.einkMode) return
      if (this.hasEinkSectionContent(this.activeSection)) return

      const firstWithContent = this.einkSectionTabs
        .map(it => it.value)
        .find(it => this.hasEinkSectionContent(it))

      if (firstWithContent) {
        this.activeSection = firstWithContent
      }
    },
    getSectionConfig(section: RecommendedViewSection): SectionConfig | undefined {
      switch (section) {
        case RecommendedViewSection.TRENDING:
          return {
            loader: undefined,
            type: SectionType.SERIES,
            value: section,
          }
        case RecommendedViewSection.KEEP_READING:
          return {
            loader: this.loaderKeepReadingBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.SHOW_SERIES],
          }
        case RecommendedViewSection.ON_DECK:
          return {
            loader: this.loaderOnDeckBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.SHOW_SERIES],
          }
        case RecommendedViewSection.RECENTLY_RELEASED_BOOKS:
          return {
            loader: this.loaderRecentlyReleasedBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.RELEASE_DATE, ItemContext.SHOW_SERIES],
          }
        case RecommendedViewSection.RECENTLY_ADDED_BOOKS:
          return {
            loader: this.loaderRecentlyAddedBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.SHOW_SERIES],
          }
        case RecommendedViewSection.RECENTLY_ADDED_SERIES:
          return {
            loader: this.loaderRecentlyAddedSeries,
            type: SectionType.SERIES,
            value: section,
          }
        case RecommendedViewSection.RECENTLY_UPDATED_SERIES:
          return {
            loader: this.loaderRecentlyUpdatedSeries,
            type: SectionType.SERIES,
            value: section,
          }
        case RecommendedViewSection.RECENTLY_READ_BOOKS:
          return {
            loader: this.loaderRecentlyReadBooks,
            type: SectionType.BOOK,
            value: section,
            itemContext: [ItemContext.SHOW_SERIES],
          }
        default:
          return undefined
      }
    },
    async scrollChanged(loader: PageLoader<any>, percent: number) {
      if (percent > 0.95) await loader.loadNext()
    },
    async loadTrendingSeries() {
      try {
        const ranked = await this.$komgaReadingStats.getTopSeriesByPeriod('weekly', 12)
        const cards = await Promise.all(
          ranked.map(async (it: TopSeriesReadingStatAggregateDto) => {
            try {
              return await this.$komgaSeries.getOneSeries(it.seriesId)
            } catch (e) {
              return undefined
            }
          }),
        )
        this.trendingSeries = cards.filter((it): it is SeriesDto => it !== undefined)
      } catch (e) {
        this.trendingSeries = []
      }
    },
    getRequestLibraryId(libraryId: string): string[] {
      return libraryId !== LIBRARIES_ALL ? [libraryId] : this.$store.getters.getLibrariesPinned.map((it: LibraryDto) => it.id)
    },
    seriesChanged(event: SeriesSseDto) {
      if (this.libraryId === LIBRARIES_ALL || event.libraryId === this.libraryId) {
        this.reload()
      }
    },
    bookChanged(event: BookSseDto) {
      if (this.libraryId === LIBRARIES_ALL || event.libraryId === this.libraryId) {
        this.reload()
      }
    },
    readProgressChanged(event: ReadProgressSseDto) {
      if (this.loaderKeepReadingBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderRecentlyAddedBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderOnDeckBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderRecentlyReleasedBooks?.items.some(b => b.id === event.bookId)) this.reload()
      else if (this.loaderRecentlyReadBooks?.items.some(b => b.id === event.bookId)) this.reload()
    },
    readProgressSeriesChanged(event: ReadProgressSeriesSseDto) {
      if (this.loaderRecentlyUpdatedSeries?.items.some(s => s.id === event.seriesId)) this.reload()
      else if (this.loaderRecentlyAddedSeries?.items.some(s => s.id === event.seriesId)) this.reload()
    },
    reload: throttle(function (this: any) {
      this.loadAll(true)
    }, 5000),
    setupLoaders(libraryId: string) {
      const requestLibraries = this.getRequestLibraryId(libraryId)
      const baseBookConditions = [] as SearchConditionBook[]
      if (requestLibraries)
        baseBookConditions.push(new SearchConditionAnyOfSeries(
          requestLibraries.map((it: string) => new SearchConditionLibraryId(new SearchOperatorIs(it))),
        ))

      this.loaderKeepReadingBooks = this.hasSection(RecommendedViewSection.KEEP_READING) ? new PageLoader<BookDto>(
        {sort: ['readProgress.readDate,desc']},
        (pageable: PageRequest) => this.$komgaBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.IN_PROGRESS))]),
        } as BookSearch, pageable),
      ) : undefined
      this.loaderOnDeckBooks = this.hasSection(RecommendedViewSection.ON_DECK) ? new PageLoader<BookDto>(
        {},
        (pageable: PageRequest) => this.$komgaBooks.getBooksOnDeck(requestLibraries, pageable),
      ) : undefined
      this.loaderRecentlyAddedBooks = this.hasSection(RecommendedViewSection.RECENTLY_ADDED_BOOKS) ? new PageLoader<BookDto>(
        {sort: ['createdDate,desc']},
        (pageable: PageRequest) => this.$komgaBooks.getBooksList({
          condition: new SearchConditionAllOfBook(baseBookConditions),
        } as BookSearch, pageable),
      ) : undefined
      this.loaderRecentlyReleasedBooks = this.hasSection(RecommendedViewSection.RECENTLY_RELEASED_BOOKS) ? new PageLoader<BookDto>(
        {sort: ['metadata.releaseDate,desc']},
        (pageable: PageRequest) => this.$komgaBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReleaseDate(new SearchOperatorAfter(subMonths(new Date(), 1)))]),
        } as BookSearch, pageable),
      ) : undefined
      this.loaderRecentlyReadBooks = this.hasSection(RecommendedViewSection.RECENTLY_READ_BOOKS) ? new PageLoader<BookDto>(
        {sort: ['readProgress.readDate,desc']},
        (pageable: PageRequest) => this.$komgaBooks.getBooksList({
          condition: new SearchConditionAllOfBook([...baseBookConditions, new SearchConditionReadStatus(new SearchOperatorIs(ReadStatus.READ))]),
        } as BookSearch, pageable),
      ) : undefined

      this.loaderRecentlyAddedSeries = this.hasSection(RecommendedViewSection.RECENTLY_ADDED_SERIES) ? new PageLoader<SeriesDto>(
        {},
        (pageable: PageRequest) => this.$komgaSeries.getNewSeries(requestLibraries, false, pageable),
      ) : undefined
      this.loaderRecentlyUpdatedSeries = this.hasSection(RecommendedViewSection.RECENTLY_UPDATED_SERIES) ? new PageLoader<SeriesDto>(
        {},
        (pageable: PageRequest) => this.$komgaSeries.getUpdatedSeries(requestLibraries, false, pageable),
      ) : undefined
    },
    loadAll(reload: boolean = false) {
      this.loading = true
      if (this.library != undefined) document.title = `Komga - ${this.library.name}`
      this.selectedSeries = []
      this.selectedBooks = []

      const requests = reload ? [
        this.loadTrendingSeries(),
        this.loaderKeepReadingBooks?.reload(),
        this.loaderOnDeckBooks?.reload(),
        this.loaderRecentlyReleasedBooks?.reload(),
        this.loaderRecentlyAddedBooks?.reload(),
        this.loaderRecentlyAddedSeries?.reload(),
        this.loaderRecentlyUpdatedSeries?.reload(),
        this.loaderRecentlyReadBooks?.reload(),
      ] : [
        this.loadTrendingSeries(),
        this.loaderKeepReadingBooks?.loadNext(),
        this.loaderOnDeckBooks?.loadNext(),
        this.loaderRecentlyReleasedBooks?.loadNext(),
        this.loaderRecentlyAddedBooks?.loadNext(),
        this.loaderRecentlyAddedSeries?.loadNext(),
        this.loaderRecentlyUpdatedSeries?.loadNext(),
        this.loaderRecentlyReadBooks?.loadNext(),
      ]

      Promise.allSettled(requests).finally(() => {
        this.syncEinkSectionWithContent()
        this.loading = false
      })
    },
    async singleEditSeries(series: SeriesDto) {
      if (series.oneshot) {
        let book = (await this.$komgaBooks.getBooksList({
          condition: new SearchConditionSeriesId(new SearchOperatorIs(series.id)),
        } as BookSearch)).content[0]
        this.$store.dispatch('dialogUpdateOneshots', {series: series, book: book})
      } else
        this.$store.dispatch('dialogUpdateSeries', series)
    },
    async singleEditBook(book: BookDto) {
      if (book.oneshot) {
        const series = (await this.$komgaSeries.getOneSeries(book.seriesId))
        this.$store.dispatch('dialogUpdateOneshots', {series: series, book: book})
      } else
        this.$store.dispatch('dialogUpdateBooks', book)
    },
    async markSelectedSeriesRead() {
      await Promise.all(this.selectedSeries.map(s =>
        this.$komgaSeries.markAsRead(s.id),
      ))
      this.selectedSeries = []
    },
    async markSelectedSeriesUnread() {
      await Promise.all(this.selectedSeries.map(s =>
        this.$komgaSeries.markAsUnread(s.id),
      ))
      this.selectedSeries = []
    },
    addToCollection() {
      this.$store.dispatch('dialogAddSeriesToCollection', this.selectedSeries.map(s => s.id))
      this.selectedSeries = []
    },
    addOneshotsToCollection() {
      this.$store.dispatch('dialogAddSeriesToCollection', this.selectedBooks.map(b => b.seriesId))
      this.selectedBooks = []
    },
    async editMultipleSeries() {
      if (this.selectedSeries.every(s => s.oneshot)) {
        const books = await Promise.all(this.selectedSeries.map(s => this.$komgaBooks.getBooksList({
          condition: new SearchConditionSeriesId(new SearchOperatorIs(s.id)),
        } as BookSearch)))
        const oneshots = this.selectedSeries.map((s, index) => ({series: s, book: books[index].content[0]} as Oneshot))
        this.$store.dispatch('dialogUpdateOneshots', oneshots)
      } else
        this.$store.dispatch('dialogUpdateSeries', this.selectedSeries)
    },
    async editMultipleBooks() {
      if (this.selectedBooks.every(b => b.oneshot)) {
        const series = await Promise.all(this.selectedBooks.map(b => this.$komgaSeries.getOneSeries(b.seriesId)))
        const oneshots = this.selectedBooks.map((b, index) => ({series: series[index], book: b} as Oneshot))
        this.$store.dispatch('dialogUpdateOneshots', oneshots)
      } else
        this.$store.dispatch('dialogUpdateBooks', this.selectedBooks)
    },
    deleteSeries() {
      this.$store.dispatch('dialogDeleteSeries', this.selectedSeries)
    },
    deleteBooks() {
      this.$store.dispatch('dialogDeleteBook', this.selectedBooks)
    },
    bulkEditMultipleBooks() {
      this.$store.dispatch('dialogUpdateBulkBooks', this.selectedBooks)
    },
    addToReadList() {
      this.$store.dispatch('dialogAddBooksToReadList', this.selectedBooks.map(b => b.id))
      this.selectedBooks = []
    },
    async addSeriesBooksToReadList() {
      const conditions = this.selectedSeries.map(s => new SearchConditionSeriesId(new SearchOperatorIs(s.id)))
      const books = await this.$komgaBooks.getBooksList({
        condition: new SearchConditionAnyOfBook(conditions),
      } as BookSearch, {unpaged: true})
      this.$store.dispatch('dialogAddBooksToReadList', books.content.map(b => b.id))
    },
    async markSelectedBooksRead() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$komgaBooks.updateReadProgress(b.id, {completed: true}),
      ))
      this.selectedBooks = []
    },
    async markSelectedBooksUnread() {
      await Promise.all(this.selectedBooks.map(b =>
        this.$komgaBooks.deleteReadProgress(b.id),
      ))
      this.selectedBooks = []
    },
    getLibraryLazy(libraryId: string): LibraryDto | undefined {
      if (libraryId !== LIBRARIES_ALL) {
        return this.$store.getters.getLibraryById(libraryId)
      } else {
        return undefined
      }
    },
  },
})
</script>

<style scoped>
.eink-dashboard-container {
  padding-left: 2px !important;
  padding-right: 2px !important;
  padding-top: 2px !important;
  padding-bottom: 2px !important;
}

.eink-selector-wrap {
  margin-bottom: 6px;
}

.eink-section-block {
  margin-bottom: 4px;
}

.eink-section-selector {
  max-width: 100%;
}

.eink-section-selector :deep(.v-input__slot) {
  min-height: 44px !important;
}

.eink-section-selector :deep(.v-select__selection) {
  font-size: 0.95rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
