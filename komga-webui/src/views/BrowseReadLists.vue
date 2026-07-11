<template>
  <div :style="$vuetify.breakpoint.xs ? 'margin-bottom: 56px' : undefined" :class="einkMode ? 'eink-page-root' : ''">
    <toolbar-sticky v-if="selectedReadLists.length === 0">
      <!--   Action menu   -->
      <library-actions-menu v-if="isAdmin && library"
                            :library="library"/>

      <v-toolbar-title>
        <span>{{ toolbarTitle }}</span>
        <v-chip label class="mx-4" v-if="totalElements">
          <span style="font-size: 1.1rem">{{ totalElements }}</span>
        </v-chip>
      </v-toolbar-title>

      <v-spacer/>

      <library-navigation v-if="$vuetify.breakpoint.mdAndUp" :libraryId="libraryId"/>

      <v-spacer/>

      <page-size-select v-model="pageSize"/>
    </toolbar-sticky>

    <multi-select-bar
      v-model="selectedReadLists"
      kind="readlists"
      show-select-all
      @unselect-all="selectedReadLists = []"
      @select-all="selectedReadLists = readLists"
      @delete="deleteReadLists"
    />

    <library-navigation v-if="$vuetify.breakpoint.smAndDown && !einkMode" :libraryId="libraryId" bottom-navigation/>

    <v-container fluid :class="einkMode ? 'eink-content-max' : ''">
      <template v-if="einkMode">
        <eink-item-browser
          :items="readLists"
          :reserved-height="einkReservedHeight"
          :external-pager-active="einkMode && totalPages > 1"
        />

        <v-pagination
          v-if="totalPages > 1"
          v-model="page"
          :total-visible="paginationVisible"
          :length="totalPages"
          :class="einkMode ? 'eink-bottom-pagination' : ''"
        />
      </template>
      <template v-else>
        <v-pagination
          v-if="totalPages > 1"
          v-model="page"
          :total-visible="paginationVisible"
          :length="totalPages"
        />

        <item-browser
          :items="readLists"
          :selectable="isAdmin"
          :selected.sync="selectedReadLists"
          :edit-function="isAdmin ? editSingle : undefined"
        />

        <v-pagination
          v-if="totalPages > 1"
          v-model="page"
          :total-visible="paginationVisible"
          :length="totalPages"
        />
      </template>
    </v-container>

  </div>
</template>

<script lang="ts">
import ToolbarSticky from '@/components/bars/ToolbarSticky.vue'
import ItemBrowser from '@/components/ItemBrowser.vue'
import EinkItemBrowser from '@/components/EinkItemBrowser.vue'
import LibraryNavigation from '@/components/LibraryNavigation.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import PageSizeSelect from '@/components/PageSizeSelect.vue'
import {LIBRARY_CHANGED, READLIST_ADDED, READLIST_CHANGED, READLIST_DELETED} from '@/types/events'
import Vue from 'vue'
import {Location} from 'vue-router'
import {computeEinkColumns, computeEinkCompactMode, computeEinkReservedHeight, computeEinkRows} from '@/functions/eink-layout'
import {LIBRARIES_ALL, LIBRARY_ROUTE} from '@/types/library'
import {LibrarySseDto} from '@/types/komga-sse'
import MultiSelectBar from '@/components/bars/MultiSelectBar.vue'
import {LibraryDto} from '@/types/komga-libraries'
import {ReadListDto} from '@/types/komga-readlists'

export default Vue.extend({
  name: 'BrowseReadLists',
  components: {
    LibraryActionsMenu,
    ToolbarSticky,
    LibraryNavigation,
    ItemBrowser,
    EinkItemBrowser,
    PageSizeSelect,
    MultiSelectBar,
  },
  data: () => {
    return {
      readLists: [] as ReadListDto[],
      selectedReadLists: [] as ReadListDto[],
      page: 1,
      pageSize: 20,
      totalPages: 1,
      totalElements: null as number | null,
      pageUnwatch: null as any,
      pageSizeUnwatch: null as any,
    }
  },
  props: {
    libraryId: {
      type: String,
      default: LIBRARIES_ALL,
    },
  },
  watch: {
    einkAutoPageSize() {
      this.applyEinkPageSize()
    },
    einkMode() {
      this.applyEinkPageSize()
    },
    '$store.getters.getLibrariesPinned': {
      handler(val) {
        if (this.libraryId === LIBRARIES_ALL)
          this.loadLibrary(this.libraryId)
      },
    },
  },
  created() {
    this.$eventHub.$on(READLIST_ADDED, this.reloadElements)
    this.$eventHub.$on(READLIST_CHANGED, this.reloadElements)
    this.$eventHub.$on(READLIST_DELETED, this.reloadElements)
    this.$eventHub.$on(LIBRARY_CHANGED, this.reloadLibrary)
  },
  beforeDestroy() {
    this.$eventHub.$off(READLIST_ADDED, this.reloadElements)
    this.$eventHub.$off(READLIST_CHANGED, this.reloadElements)
    this.$eventHub.$off(READLIST_DELETED, this.reloadElements)
    this.$eventHub.$off(LIBRARY_CHANGED, this.reloadLibrary)
  },
  mounted() {
    this.$store.commit('setLibraryRoute', {id: this.libraryId, route: LIBRARY_ROUTE.READLISTS})
    this.pageSize = this.$store.state.persistedState.browsingPageSize || this.pageSize

    // restore from query param
    if (this.$route.query.page) this.page = Number(this.$route.query.page)
    if (this.$route.query.pageSize) this.pageSize = Number(this.$route.query.pageSize)

    this.applyEinkPageSize()

    this.loadLibrary(this.libraryId)

    this.setWatches()
  },
  beforeRouteUpdate(to, from, next) {
    if (to.params.libraryId !== from.params.libraryId) {
      // reset
      this.page = 1
      this.totalPages = 1
      this.totalElements = null
      this.readLists = []

      this.loadLibrary(to.params.libraryId)
    }

    next()
  },
  computed: {
    library(): LibraryDto | undefined {
      return this.getLibraryLazy(this.libraryId)
    },
    toolbarTitle(): string {
      if (this.library) return this.library.name
      else if (this.$store.getters.getLibrariesPinned.length > 0) return this.$t('common.pinned_libraries').toString()
      else return this.$t('common.all_libraries').toString()
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    einkMode(): boolean {
      return this.$store.state.persistedState.theme === 'theme.eink'
    },
    einkAutoPageSize(): number {
      if (!this.einkMode) return this.pageSize
      return this.einkColumns * this.einkRows
    },
    einkCompactMode(): boolean {
      return computeEinkCompactMode(this.$vuetify.breakpoint.width, this.$vuetify.breakpoint.height)
    },
    einkColumns(): number {
      return computeEinkColumns({
        width: this.$vuetify.breakpoint.width,
        height: this.$vuetify.breakpoint.height,
        compactMode: this.einkCompactMode,
        minColumnsPortrait: 3,
        minColumnsLandscape: 3,
        maxColumns: 5,
        minCardWidthCompactPortrait: 108,
        minCardWidthCompactLandscape: 118,
        minCardWidthPortrait: 136,
        minCardWidthLandscape: 160,
        horizontalPaddingCompact: 8,
        horizontalPaddingRegular: 12,
      })
    },
    einkRows(): number {
      return computeEinkRows({
        width: this.$vuetify.breakpoint.width,
        height: this.$vuetify.breakpoint.height,
        compactMode: this.einkCompactMode,
        columns: this.einkColumns,
        reservedHeight: this.einkReservedHeight,
        minRowsPortrait: 2,
        minRowsLandscape: 1,
        maxRows: 6,
        baseOffset: 64,
        gridPaddingPortraitCompact: 10,
        gridPaddingPortraitRegular: 14,
        gridPaddingLandscape: 8,
        safetyPaddingPortraitCompact: 10,
        safetyPaddingPortraitRegular: 14,
        safetyPaddingLandscape: 6,
        cardMetaHeightCompact: 34,
        cardMetaHeightRegular: 40,
        rowSafetyRatioPortrait: 1.1,
        rowSafetyRatioLandscape: 1.06,
        itemWidthInsetCompact: 4,
        itemWidthInsetRegular: 6,
        horizontalPaddingCompact: 8,
        horizontalPaddingRegular: 12,
      })
    },
    einkReservedHeight(): number {
      if (!this.einkMode) return 220
      return computeEinkReservedHeight({
        width: this.$vuetify.breakpoint.width,
        height: this.$vuetify.breakpoint.height,
        shortSideThresholds: [430, 540],
        portraitValues: [140, 160, 180],
        landscapeValues: [100, 110, 130],
      })
    },
    paginationVisible(): number {
      switch (this.$vuetify.breakpoint.name) {
        case 'xs':
          return 5
        case 'sm':
        case 'md':
          return 10
        case 'lg':
        case 'xl':
        default:
          return 15
      }
    },
  },
  methods: {
    applyEinkPageSize() {
      if (!this.einkMode) return
      if (this.pageSize !== this.einkAutoPageSize) {
        this.pageSize = this.einkAutoPageSize
      }
    },
    setWatches() {
      this.pageSizeUnwatch = this.$watch('pageSize', (val) => {
        this.$store.commit('setBrowsingPageSize', val)
        this.updateRouteAndReload()
      })

      this.pageUnwatch = this.$watch('page', (val) => {
        this.updateRoute()
        this.loadPage(this.libraryId, val)
      })
    },
    unsetWatches() {
      this.pageUnwatch()
      this.pageSizeUnwatch()
    },
    updateRouteAndReload() {
      this.unsetWatches()

      this.page = 1

      this.updateRoute()
      this.loadPage(this.libraryId, this.page)

      this.setWatches()
    },
    updateRoute() {
      this.$router.replace({
        name: this.$route.name,
        params: {libraryId: this.$route.params.libraryId},
        query: {
          page: `${this.page}`,
          pageSize: `${this.pageSize}`,
        },
      } as Location).catch((_: any) => {
      })
    },
    reloadElements() {
      this.loadLibrary(this.libraryId)
    },
    reloadLibrary(event: LibrarySseDto) {
      if (event.libraryId === this.libraryId) {
        this.loadLibrary(this.libraryId)
      }
    },
    async loadLibrary(libraryId: string) {
      if (this.library != undefined) document.title = `Komga - ${this.library.name}`
      await this.loadPage(libraryId, this.page)

      if (this.totalElements === 0) {
        await this.$router.push({name: 'browse-libraries', params: {libraryId: libraryId.toString()}})
      }
    },
    async loadPage(libraryId: string, page: number) {
      this.selectedReadLists = []

      const pageRequest = {
        page: page - 1,
        size: this.pageSize,
      } as PageRequest

      const lib = libraryId !== LIBRARIES_ALL ? [libraryId] : this.$store.getters.getLibrariesPinned.map(it => it.id)
      const elementsPage = await this.$komgaReadLists.getReadLists(lib, pageRequest)

      this.totalPages = elementsPage.totalPages
      this.totalElements = elementsPage.totalElements
      this.readLists = elementsPage.content
    },
    getLibraryLazy(libraryId: string): LibraryDto | undefined {
      if (libraryId !== LIBRARIES_ALL) {
        return this.$store.getters.getLibraryById(libraryId)
      } else {
        return undefined
      }
    },
    editSingle(element: ReadListDto) {
      this.$store.dispatch('dialogEditReadList', element)
    },
    deleteReadLists() {
      this.$store.dispatch('dialogDeleteReadList', this.selectedReadLists)
    },
  },
})
</script>

<style scoped>
:deep(.theme--eink) .eink-page-root {
  padding-bottom: 60px;
}

:deep(.theme--eink) .eink-bottom-pagination {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 24;
  margin: 0;
  padding: 1px 4px;
  background: #ffffff;
  border-top: 2px solid #000000;
  min-height: 32px;
  overflow: hidden;
}
</style>
