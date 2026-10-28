<template>
  <div :style="$vuetify.breakpoint.xs ? 'margin-bottom: 56px' : undefined">
    <toolbar-sticky v-if="selectedCollections.length === 0">
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

      <v-btn icon @click="createCollection" v-if="isLoggedIn">
        <v-tooltip bottom>
          <template v-slot:activator="{ on }">
            <v-icon v-on="on">mdi-plus</v-icon>
          </template>
          {{ $t('common.add') }}
        </v-tooltip>
      </v-btn>

      <page-size-select v-model="pageSize"/>
    </toolbar-sticky>

    <multi-select-bar
      v-model="selectedCollections"
      kind="collections"
      show-select-all
      @unselect-all="selectedCollections = []"
      @select-all="selectedCollections = collections"
      @delete="deleteCollections"
    />

    <library-navigation v-if="$vuetify.breakpoint.smAndDown" :libraryId="libraryId" bottom-navigation/>

    <v-container fluid>
      <v-checkbox
        v-model="showPublicCollections"
        :label="$t('browse_collections.checkbox_show_public_collections')"
        class="mb-2"
        @change="loadPage(libraryId, 1)"
      />

      <v-pagination
        v-if="totalPages > 1"
        v-model="page"
        :total-visible="paginationVisible"
        :length="totalPages"
      />

      <item-browser
        :items="collections"
        :selectable="isAdmin"
        :selected.sync="selectedCollections"
        :edit-function="isAdmin ? editSingleCollection : undefined"
      />

      <v-pagination
        v-if="totalPages > 1"
        v-model="page"
        :total-visible="paginationVisible"
        :length="totalPages"
      />
    </v-container>

  </div>
</template>

<script lang="ts">
import ToolbarSticky from '@/components/bars/ToolbarSticky.vue'
import ItemBrowser from '@/components/ItemBrowser.vue'
import LibraryNavigation from '@/components/LibraryNavigation.vue'
import LibraryActionsMenu from '@/components/menus/LibraryActionsMenu.vue'
import PageSizeSelect from '@/components/PageSizeSelect.vue'
import {COLLECTION_ADDED, COLLECTION_CHANGED, COLLECTION_DELETED, LIBRARY_CHANGED} from '@/types/events'
import Vue from 'vue'
import {Location} from 'vue-router'
import {LIBRARIES_ALL, LIBRARY_ROUTE} from '@/types/library'
import {LibrarySseDto} from '@/types/komga-sse'
import MultiSelectBar from '@/components/bars/MultiSelectBar.vue'
import {LibraryDto} from '@/types/komga-libraries'
import {CollectionDto, CollectionCreationDto, CollectionUpdateDto, CollectionThumbnailDto} from '@/types/komga-collections'
import {Page, PageRequest} from '@/types/pageable'

// Define the component interface
interface BrowseCollectionsData {
  collections: CollectionDto[]
  selectedCollections: CollectionDto[]
  page: number
  pageSize: number
  totalPages: number
  totalElements: number | null
  pageUnwatch: any
  pageSizeUnwatch: any
  showPublicCollections: boolean
}

interface BrowseCollectionsComputed {
  library: LibraryDto | undefined
  toolbarTitle: string
  isAdmin: boolean
  isLoggedIn: boolean
  paginationVisible: number
}

interface BrowseCollectionsMethods {
  setWatches(): void
  unsetWatches(): void
  updateRouteAndReload(): void
  updateRoute(): void
  reloadCollections(): void
  reloadLibrary(event: LibrarySseDto): void
  loadLibrary(libraryId: string): Promise<void>
  loadPage(libraryId: string, page: number): Promise<void>
  getLibraryLazy(libraryId: string): LibraryDto | undefined
  editSingleCollection(collection: CollectionDto): void
  createCollection(): void
  deleteCollections(): void
}

// Extend Vue with additional properties for type safety
declare module 'vue' {
  interface ComponentCustomProperties {
    $komgaCollections: ReturnType<typeof import('@/services/komga-collections.service').default>
    $eventHub: Vue
    $store: any
    $route: any
    $router: any
    $vuetify: any
    $t: any
  }
}

export default Vue.extend({
  name: 'BrowseCollections',
  components: {
    LibraryActionsMenu,
    ToolbarSticky,
    LibraryNavigation,
    ItemBrowser,
    PageSizeSelect,
    MultiSelectBar,
  },
  data() {
    return {
      collections: [] as CollectionDto[],
      selectedCollections: [] as CollectionDto[],
      page: 1,
      pageSize: 20,
      totalPages: 1,
      totalElements: null as number | null,
      pageUnwatch: null as any,
      pageSizeUnwatch: null as any,
      showPublicCollections: true,
    }
  },
  props: {
    libraryId: {
      type: String,
      default: LIBRARIES_ALL,
    },
  },
  computed: {
    library(): LibraryDto | undefined {
      if (this.libraryId !== LIBRARIES_ALL) {
        return this.$store.getters.getLibraryById(this.libraryId)
      } else {
        return undefined
      }
    },
    toolbarTitle(): string {
      if (this.library) return this.library.name
      else if (this.$store.getters.getLibrariesPinned.length > 0) return this.$t('common.pinned_libraries').toString()
      else return this.$t('common.all_libraries').toString()
    },
    isAdmin(): boolean {
      return this.$store.getters.meAdmin
    },
    isLoggedIn(): boolean {
      return this.$store.getters.me !== undefined
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
  watch: {
    '$store.getters.getLibrariesPinned': function (val) {
      if (this.libraryId === LIBRARIES_ALL)
        this.loadLibrary(this.libraryId)
    },
  },
  created() {
    this.$eventHub.$on(COLLECTION_ADDED, this.reloadCollections)
    this.$eventHub.$on(COLLECTION_CHANGED, this.reloadCollections)
    this.$eventHub.$on(COLLECTION_DELETED, this.reloadCollections)
    this.$eventHub.$on(LIBRARY_CHANGED, this.reloadLibrary)
  },
  beforeDestroy() {
    this.$eventHub.$off(COLLECTION_ADDED, this.reloadCollections)
    this.$eventHub.$off(COLLECTION_CHANGED, this.reloadCollections)
    this.$eventHub.$off(COLLECTION_DELETED, this.reloadCollections)
    this.$eventHub.$off(LIBRARY_CHANGED, this.reloadLibrary)
  },
  mounted() {
    this.$store.commit('setLibraryRoute', {id: this.libraryId, route: LIBRARY_ROUTE.COLLECTIONS})
    this.pageSize = this.$store.state.persistedState.browsingPageSize || this.pageSize

    // restore from query param
    if (this.$route.query.page) this.page = Number(this.$route.query.page)
    if (this.$route.query.pageSize) this.pageSize = Number(this.$route.query.pageSize)

    this.loadLibrary(this.libraryId)

    this.setWatches()
  },
  beforeRouteUpdate(to: any, from: any, next: any) {
    if (to.params.libraryId !== from.params.libraryId) {
      // reset
      this.page = 1
      this.totalPages = 1
      this.totalElements = null
      this.collections = []

      this.loadLibrary(to.params.libraryId)
    }

    next()
  },
  methods: {
    setWatches() {
      this.pageSizeUnwatch = this.$watch('pageSize', (val: any) => {
        this.$store.commit('setBrowsingPageSize', val)
        this.updateRouteAndReload()
      })

      this.pageUnwatch = this.$watch('page', (val: any) => {
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
    reloadCollections() {
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
      this.selectedCollections = []

      const pageRequest: PageRequest = {
        page: page - 1,
        size: this.pageSize,
      }

      const lib = libraryId !== LIBRARIES_ALL ? [libraryId] : this.$store.getters.getLibrariesPinned.map((it: any) => it.id)
      const collectionsPage = await this.$komgaCollections.getCollections(lib, pageRequest, undefined, this.showPublicCollections)

      this.totalPages = collectionsPage.totalPages
      this.totalElements = collectionsPage.totalElements
      this.collections = collectionsPage.content
    },
    getLibraryLazy(libraryId: string): LibraryDto | undefined {
      if (libraryId !== LIBRARIES_ALL) {
        return this.$store.getters.getLibraryById(libraryId)
      } else {
        return undefined
      }
    },
    editSingleCollection(collection: CollectionDto) {
      this.$store.dispatch('dialogEditCollection', collection)
    },
    createCollection() {
      this.$store.dispatch('dialogCreateCollection')
    },
    deleteCollections() {
      this.$store.dispatch('dialogDeleteCollection', this.selectedCollections)
    },
  },
})
</script>
