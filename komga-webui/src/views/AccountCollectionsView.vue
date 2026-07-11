<template>
  <v-container fluid>
    <toolbar-sticky>
      <v-toolbar-title>
        <span>{{ $t('common.collections') }}</span>
      </v-toolbar-title>

      <v-spacer/>

      <v-btn color="primary" @click="showCreateDialog = true">
        <v-icon left>mdi-plus</v-icon>
        {{ $t('common.create') }}
      </v-btn>
    </toolbar-sticky>

    <v-row class="mt-2">
      <v-col cols="12" md="4">
        <v-card outlined>
          <v-list two-line>
            <v-list-item
              v-for="collection in collections"
              :key="collection.id"
              :color="collection.id === selectedCollectionId ? 'primary' : undefined"
              @click="selectCollection(collection.id)"
            >
              <v-list-item-content>
                <v-list-item-title>{{ collection.name }}</v-list-item-title>
                <v-list-item-subtitle>{{ $tc('common.books_n', collection.seriesIds.length, {count: collection.seriesIds.length}) }}</v-list-item-subtitle>
              </v-list-item-content>

              <v-list-item-action>
                <v-checkbox
                  :input-value="collection.showInSidebar"
                  hide-details
                  class="mt-0 pt-0"
                  @click.stop
                  @change="toggleShowInSidebar(collection, $event)"
                />
              </v-list-item-action>

              <v-list-item-action>
                <v-btn icon :to="{name: 'browse-collection', params: {collectionId: collection.id}}">
                  <v-icon>mdi-open-in-new</v-icon>
                </v-btn>
              </v-list-item-action>

              <v-list-item-action>
                <v-btn icon color="error" @click.stop="confirmDeleteCollection(collection)">
                  <v-icon>mdi-delete</v-icon>
                </v-btn>
              </v-list-item-action>
            </v-list-item>
          </v-list>

          <v-card-text v-if="collections.length === 0" class="text--secondary">
            {{ $t('common.nothing_to_show') }}
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" md="8">
        <v-card outlined>
          <v-list v-if="selectedCollectionSeries.length > 0">
            <v-list-item v-for="series in selectedCollectionSeries" :key="series.id">
              <v-list-item-content>
                <v-list-item-title>{{ series.name }}</v-list-item-title>
                <v-list-item-subtitle>{{ $tc('common.books_n', series.booksCount, {count: series.booksCount}) }}</v-list-item-subtitle>
              </v-list-item-content>

              <v-list-item-action>
                <v-btn icon color="error" @click.stop.prevent="removeFromCollection(series.id)">
                  <v-icon>mdi-delete</v-icon>
                </v-btn>
              </v-list-item-action>
            </v-list-item>
          </v-list>

          <v-card-text v-else class="text--secondary">
            {{ $t('common.nothing_to_show') }}
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-dialog v-model="showCreateDialog" max-width="500">
      <v-card>
        <v-card-title>{{ $t('common.create') }} {{ $t('common.collections') }}</v-card-title>
        <v-card-text>
          <v-text-field
            v-model="newCollectionName"
            :label="$t('common.name')"
            autofocus
          />
        </v-card-text>
        <v-card-actions>
          <v-spacer/>
          <v-btn text @click="showCreateDialog = false">{{ $t('common.cancel') }}</v-btn>
          <v-btn color="primary" :disabled="!newCollectionName.trim()" @click="createCollection">{{ $t('common.create') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-dialog v-model="showDeleteDialog" max-width="500">
      <v-card>
        <v-card-title>{{ $t('common.delete') }} {{ $t('common.collections') }}</v-card-title>
        <v-card-text>
          {{ selectedCollection ? selectedCollection.name : '' }}
        </v-card-text>
        <v-card-actions>
          <v-spacer/>
          <v-btn text @click="showDeleteDialog = false">{{ $t('common.cancel') }}</v-btn>
          <v-btn color="error" @click="deleteCollection" :disabled="!selectedCollection">{{ $t('common.delete') }}</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script lang="ts">
import ToolbarSticky from '@/components/bars/ToolbarSticky.vue'
import {COLLECTION_ADDED, COLLECTION_CHANGED, COLLECTION_DELETED, ERROR, ErrorEvent} from '@/types/events'
import {SeriesDto} from '@/types/komga-series'
import Vue from 'vue'

export default Vue.extend({
  name: 'AccountCollectionsView',
  components: {
    ToolbarSticky,
  },
  data: () => {
    return {
      collections: [] as CollectionDto[],
      selectedCollectionId: '' as string,
      selectedCollectionSeries: [] as SeriesDto[],
      showCreateDialog: false,
      showDeleteDialog: false,
      newCollectionName: '',
    }
  },
  computed: {
    selectedCollection(): CollectionDto | undefined {
      return this.collections.find(c => c.id === this.selectedCollectionId)
    },
  },
  created() {
    this.loadCollections()
    this.$eventHub.$on(COLLECTION_ADDED, this.loadCollections)
    this.$eventHub.$on(COLLECTION_CHANGED, this.loadCollections)
    this.$eventHub.$on(COLLECTION_DELETED, this.loadCollections)
  },
  beforeDestroy() {
    this.$eventHub.$off(COLLECTION_ADDED, this.loadCollections)
    this.$eventHub.$off(COLLECTION_CHANGED, this.loadCollections)
    this.$eventHub.$off(COLLECTION_DELETED, this.loadCollections)
  },
  methods: {
    async loadCollections() {
      try {
        const collections = (await this.$komgaCollections.getCollections(undefined, {unpaged: true} as PageRequest)).content
        this.collections = collections.sort((a, b) => a.name.localeCompare(b.name, this.$i18n.locale))

        if (this.collections.length === 0) {
          this.selectedCollectionId = ''
          this.selectedCollectionSeries = []
          return
        }

        if (!this.selectedCollectionId || !this.collections.some(c => c.id === this.selectedCollectionId)) {
          this.selectedCollectionId = this.collections[0].id
        }

        await this.loadSelectedCollectionSeries()
      } catch (e) {
        this.collections = []
        this.selectedCollectionSeries = []
        this.emitError(e)
      }
    },
    async selectCollection(collectionId: string) {
      this.selectedCollectionId = collectionId
      await this.loadSelectedCollectionSeries()
    },
    async loadSelectedCollectionSeries() {
      if (!this.selectedCollectionId) {
        this.selectedCollectionSeries = []
        return
      }

      try {
        const page = await this.$komgaCollections.getSeries(this.selectedCollectionId, {unpaged: true} as PageRequest)
        this.selectedCollectionSeries = page.content
      } catch (e) {
        this.selectedCollectionSeries = []
        this.emitError(e)
      }
    },
    async createCollection() {
      if (!this.newCollectionName.trim()) return
      try {
        const created = await this.$komgaCollections.postCollection({
          name: this.newCollectionName.trim(),
          ordered: false,
          seriesIds: [],
        } as CollectionCreationDto)
        this.showCreateDialog = false
        this.newCollectionName = ''
        await this.loadCollections()
        this.selectedCollectionId = created.id
        await this.loadSelectedCollectionSeries()
      } catch (e) {
        this.emitError(e)
      }
    },
    async deleteCollection() {
      if (!this.selectedCollection) return
      try {
        await this.$komgaCollections.deleteCollection(this.selectedCollection.id)
        this.showDeleteDialog = false
        await this.loadCollections()
      } catch (e) {
        this.emitError(e)
      }
    },
    confirmDeleteCollection(collection: CollectionDto) {
      this.selectedCollectionId = collection.id
      this.showDeleteDialog = true
    },
    async toggleShowInSidebar(collection: CollectionDto, val: boolean) {
      try {
        await this.$komgaCollections.patchCollection(collection.id, {
          showInSidebar: val,
        } as CollectionUpdateDto)
        await this.loadCollections()
      } catch (e) {
        this.emitError(e)
      }
    },
    async removeFromCollection(seriesId: string) {
      if (!this.selectedCollection) return
      try {
        const update = {
          seriesIds: this.selectedCollection.seriesIds.filter(x => x !== seriesId),
        } as CollectionUpdateDto
        await this.$komgaCollections.patchCollection(this.selectedCollection.id, update)
        await this.loadCollections()
      } catch (e) {
        this.emitError(e)
      }
    },
    emitError(e: any) {
      this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
    },
  },
})
</script>