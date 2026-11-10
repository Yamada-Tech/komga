<template>
  <v-dialog v-model="modal"
            :fullscreen="$vuetify.breakpoint.xsOnly"
            max-width="800"
            @keydown.esc="dialogCancel"
  >
    <form novalidate>
      <v-card>
        <v-toolbar class="hidden-sm-and-up">
          <v-btn icon @click="dialogCancel">
            <v-icon>mdi-close</v-icon>
          </v-btn>
          <v-toolbar-title>{{ dialogTitle }}</v-toolbar-title>
          <v-spacer/>
          <v-toolbar-items>
            <v-btn text color="primary" @click="dialogConfirm">{{ $t('dialog.edit_series.button_confirm') }}</v-btn>
          </v-toolbar-items>
        </v-toolbar>

        <v-card-title class="hidden-xs-only">
          <v-icon class="mx-4">mdi-pencil</v-icon>
          {{ dialogTitle }}
        </v-card-title>

        <v-tabs :vertical="$vuetify.breakpoint.smAndUp" v-model="tab">
          <v-tab class="justify-start">
            <v-icon left class="hidden-xs-only">mdi-format-align-center</v-icon>
            {{ $t('dialog.edit_series.tab_general') }}
          </v-tab>
          <v-tab class="justify-start">
            <v-icon left class="hidden-xs-only">mdi-tag-multiple</v-icon>
            {{ $t('dialog.edit_series.tab_tags') }}
          </v-tab>

          <!--  Tab: General  -->
          <v-tab-item>
            <v-card flat>
              <v-container fluid>

                <!--  Title  -->
                <v-row v-if="single">
                  <v-col cols="12">
                    <v-text-field v-model="form.title"
                                  :label="$t('dialog.edit_series.field_title')"
                                  filled
                                  dense
                                  :hint="$t('dialog.edit_series.field_title_hint')"
                                  persistent-hint
                                  :error-messages="requiredErrors('title')"
                                  @input="$v.form.title.$touch()"
                                  @blur="$v.form.title.$touch()"
                                  @change="form.titleLock = true"
                    >
                      <template v-slot:prepend>
                        <v-icon :color="form.titleLock ? 'secondary' : ''"
                                @click="form.titleLock = !form.titleLock"
                        >
                          {{ form.titleLock ? 'mdi-lock' : 'mdi-lock-open' }}
                        </v-icon>
                      </template>
                    </v-text-field>
                  </v-col>
                </v-row>

                <!--  Sort Title  -->
                <v-row v-if="single">
                  <v-col cols="12">
                    <v-select :items="sortTitleOptions"
                              v-model="form.titleSort"
                              :label="$t('dialog.edit_series.field_sort_title')"
                              filled
                              dense
                              clearable
                              @input="$v.form.titleSort.$touch()"
                              @change="form.titleSortLock = true"
                    >
                      <template v-slot:prepend>
                        <v-icon :color="form.titleSortLock ? 'secondary' : ''"
                                @click="form.titleSortLock = !form.titleSortLock"
                        >
                          {{ form.titleSortLock ? 'mdi-lock' : 'mdi-lock-open' }}
                        </v-icon>
                      </template>
                    </v-select>
                  </v-col>
                </v-row>

                <!--  Summary  -->
                <v-row v-if="single">
                  <v-col cols="12">
                    <v-textarea v-model="form.summary"
                                :label="$t('dialog.edit_series.field_summary')"
                                filled
                                dense
                                @input="$v.form.summary.$touch()"
                                @change="form.summaryLock = true"
                    >
                      <template v-slot:prepend>
                        <v-icon :color="form.summaryLock ? 'secondary' : ''"
                                @click="form.summaryLock = !form.summaryLock"
                        >
                          {{ form.summaryLock ? 'mdi-lock' : 'mdi-lock-open' }}
                        </v-icon>
                      </template>
                    </v-textarea>
                  </v-col>
                </v-row>

                <v-row>
                  <!--  Status  -->
                  <v-col cols="6">
                    <v-select :items="seriesStatus"
                              v-model="form.status"
                              :label="$t('dialog.edit_series.field_status')"
                              filled
                              :placeholder="!single && mixed.status ? $t('dialog.edit_series.mixed') : ''"
                              @input="$v.form.status.$touch()"
                              @change="form.statusLock = true"
                    >
                      <template v-slot:prepend>
                        <v-icon :color="form.statusLock ? 'secondary' : ''"
                                @click="form.statusLock = !form.statusLock"
                        >
                          {{ form.statusLock ? 'mdi-lock' : 'mdi-lock-open' }}
                        </v-icon>
                      </template>
                    </v-select>
                  </v-col>

                  <!--  Reading Direction  -->
                  <v-col cols="6">
                    <v-select v-model="form.readingDirection"
                              :items="readingDirections"
                              :label="$t('dialog.edit_series.field_reading_direction')"
                              clearable
                              filled
                              :placeholder="!single && mixed.readingDirection ? $t('dialog.edit_series.mixed') : ''"
                              @input="$v.form.readingDirection.$touch()"
                              @change="form.readingDirectionLock = true"
                    >
                      <template v-slot:prepend>
                        <v-icon :color="form.readingDirectionLock ? 'secondary' : ''"
                                @click="form.readingDirectionLock = !form.readingDirectionLock"
                        >
                          {{ form.readingDirectionLock ? 'mdi-lock' : 'mdi-lock-open' }}
                        </v-icon>
                      </template>
                    </v-select>
                  </v-col>
                </v-row>

              </v-container>
            </v-card>
          </v-tab-item>

          <!--  Tab: Tags  -->
          <v-tab-item>
            <v-card flat>
              <v-container fluid>
                <v-alert v-if="!single"
                         type="warning"
                         outlined
                         dense
                >{{ $t('dialog.edit_series.tags_notice_multiple_edit') }}
                </v-alert>

                <!-- Genres -->
                <v-row>
                  <v-col cols="12">
                    <span class="text-body-2">{{ $t('dialog.edit_series.field_genres') }}</span>
                    <v-combobox v-model="form.genres"
                                :items="genresAvailable"
                                @input="$v.form.genres.$touch()"
                                @change="form.genresLock = true"
                                hide-selected
                                chips
                                deletable-chips
                                multiple
                                filled
                                dense
                    >
                      <template v-slot:prepend>
                        <v-icon :color="form.genresLock ? 'secondary' : ''"
                                @click="form.genresLock = !form.genresLock"
                        >
                          {{ form.genresLock ? 'mdi-lock' : 'mdi-lock-open' }}
                        </v-icon>
                      </template>
                    </v-combobox>
                  </v-col>
                </v-row>

                <!-- Tags -->
                <v-row>
                  <v-col cols="12">
                    <span class="text-body-2">{{ $t('dialog.edit_series.field_tags') }}</span>
                    <v-combobox v-model="form.tags"
                                :items="tagsAvailable"
                                @input="$v.form.tags.$touch()"
                                @change="form.tagsLock = true"
                                hide-selected
                                chips
                                deletable-chips
                                multiple
                                filled
                                dense
                    >
                      <template v-slot:prepend>
                        <v-icon :color="form.tagsLock ? 'secondary' : ''"
                                @click="form.tagsLock = !form.tagsLock"
                        >
                          {{ form.tagsLock ? 'mdi-lock' : 'mdi-lock-open' }}
                        </v-icon>
                      </template>
                    </v-combobox>
                  </v-col>
                </v-row>
              </v-container>
            </v-card>
          </v-tab-item>
        </v-tabs>

        <v-card-actions class="hidden-xs-only">
          <v-spacer/>
          <v-btn text @click="dialogCancel">{{ $t('dialog.edit_series.button_cancel') }}</v-btn>
          <v-btn color="primary" @click="dialogConfirm">{{ $t('dialog.edit_series.button_confirm') }}</v-btn>
        </v-card-actions>
      </v-card>
    </form>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'
import {SeriesStatus} from '@/types/enum-series'
import {requiredIf} from 'vuelidate/lib/validators'
import {ReadingDirection} from '@/types/enum-books'
import {SeriesDto} from '@/types/komga-series'
import {ERROR, ErrorEvent} from '@/types/events'

export default Vue.extend({
  name: 'EditSeriesDialog',
  data: () => {
    return {
      modal: false,
      tab: 0,
      form: {
        status: '',
        statusLock: false,
        title: '',
        titleLock: false,
        titleSort: '',
        titleSortLock: false,
        summary: '',
        summaryLock: false,
        readingDirection: '',
        readingDirectionLock: false,
        genres: [],
        genresLock: false,
        tags: [],
        tagsLock: false,
      },
      mixed: {
        status: false,
        readingDirection: false,
      },
      genresAvailable: [] as string[],
      tagsAvailable: [] as string[],
    }
  },
  props: {
    value: Boolean,
    series: {
      type: [Object as () => SeriesDto, Array as () => SeriesDto[]],
      required: true,
    },
  },
  watch: {
    value(val) {
      this.modal = val
    },
    modal(val) {
      if(val) {
        this.loadAvailableTags()
        this.loadAvailableGenres()
      } else {
        this.dialogCancel()
      }
    },
    series(val) {
      this.dialogReset(val)
    },
  },
  validations: {
    form: {
      title: {
        required: requiredIf(function (this: any, model: any) {
          return this.single
        }),
      },
      titleSort: {},
      status: {
        required: requiredIf(function (this: any, model: any) {
          return this.single
        }),
      },
      summary: {},
      genres: {},
      tags: {},
      readingDirection: {},
    },
  },
  computed: {
    single(): boolean {
      return !Array.isArray(this.series)
    },
    readingDirections(): any[] {
      return Object.keys(ReadingDirection).map(x => (
        {
          text: this.$t(`enums.reading_direction.${x}`),
          value: x,
        }),
      )
    },
    seriesStatus(): any[] {
      return Object.keys(SeriesStatus).map(x => ({
        text: this.$t(`enums.series_status.${x}`),
        value: x,
      }))
    },
    sortTitleOptions(): { text: string, value: string }[] {
      const latin = Array.from({length: 26}, (_, i) => String.fromCharCode(65 + i))
      const kana = 'あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん'.split('')
      return [...latin, ...kana].map(x => ({text: x, value: x}))
    },
    dialogTitle(): string {
      return this.single
        ? this.$t('dialog.edit_series.dialog_title_single', {series: this.$_.get(this.series, 'metadata.title')}).toString()
        : this.$tc('dialog.edit_series.dialog_title_multiple', (this.series as SeriesDto[]).length)
    },
  },
  methods: {
    async loadAvailableTags() {
      this.tagsAvailable = await this.$komgaReferential.getTags()
    },
    async loadAvailableGenres() {
      this.genresAvailable = await this.$komgaReferential.getGenres()
    },
    requiredErrors(fieldName: string): string[] {
      const errors = [] as string[]
      const formField = this.$v.form!![fieldName] as any
      if (!formField.$dirty) return errors
      !formField.required && errors.push(this.$t('common.required').toString())
      return errors
    },
    stripAuthorPrefix(title: string): string {
      // 先頭の [作者名] を除去（例: "[作者名] タイトル" -> "タイトル"）
      return title.replace(/^\s*\[[^\]]*\]\s*/, '')
    },
    dialogReset(series: SeriesDto | SeriesDto[]) {
      this.tab = 0
      this.$v.$reset()
      if (Array.isArray(series) && series.length === 0) return
      if (Array.isArray(series) && series.length > 0) {
        const status = this.$_.uniq(series.map(x => x.metadata.status))
        this.form.status = status.length > 1 ? '' : status[0]
        this.mixed.status = status.length > 1

        const statusLock = this.$_.uniq(series.map(x => x.metadata.statusLock))
        this.form.statusLock = statusLock.length > 1 ? false : statusLock[0]

        const readingDirection = this.$_.uniq(series.map(x => x.metadata.readingDirection))
        this.form.readingDirection = readingDirection.length > 1 ? '' : readingDirection[0]
        this.mixed.readingDirection = readingDirection.length > 1

        const readingDirectionLock = this.$_.uniq(series.map(x => x.metadata.readingDirectionLock))
        this.form.readingDirectionLock = readingDirectionLock.length > 1 ? false : readingDirectionLock[0]

        this.form.genres = []

        const genresLock = this.$_.uniq(series.map(x => x.metadata.genresLock))
        this.form.genresLock = genresLock.length > 1 ? false : genresLock[0]

        this.form.tags = []

        const tagsLock = this.$_.uniq(series.map(x => x.metadata.tagsLock))
        this.form.tagsLock = tagsLock.length > 1 ? false : tagsLock[0]
      } else {
        this.form.genres = []
        this.form.tags = []
        this.$_.merge(this.form, (series as SeriesDto).metadata)
      }
    },
    dialogCancel() {
      this.$emit('input', false)
      this.dialogReset(this.series)
    },
    async dialogConfirm() {
      if (await this.editSeries()) {
        this.$emit('input', false)
      }
    },
    validateForm(): any {
      if (!this.$v.$invalid) {
        const metadata = {
          statusLock: this.form.statusLock,
          readingDirectionLock: this.form.readingDirectionLock,
          genresLock: this.form.genresLock,
          tagsLock: this.form.tagsLock,
        }

        if (this.$v.form?.status?.$dirty) {
          this.$_.merge(metadata, {status: this.form.status})
        }

        if (this.$v.form?.readingDirection?.$dirty) {
          this.$_.merge(metadata, {readingDirection: this.form.readingDirection ? this.form.readingDirection : null})
        }

        if (this.$v.form?.genres?.$dirty) {
          this.$_.merge(metadata, {genres: this.form.genres})
        }

        if (this.$v.form?.tags?.$dirty) {
          this.$_.merge(metadata, {tags: this.form.tags})
        }

        if (this.single) {
          this.$_.merge(metadata, {
            titleLock: this.form.titleLock,
            titleSortLock: this.form.titleSortLock,
            summaryLock: this.form.summaryLock,
          })

          if (this.$v.form?.title?.$dirty) {
            this.$_.merge(metadata, {title: this.stripAuthorPrefix(this.form.title)})
          }

          if (this.$v.form?.titleSort?.$dirty) {
            this.$_.merge(metadata, {titleSort: this.form.titleSort ? this.form.titleSort : null})
          }

          if (this.$v.form?.summary?.$dirty) {
            this.$_.merge(metadata, {summary: this.form.summary})
          }
        }

        return metadata
      }
      return null
    },
    async editSeries(): Promise<boolean> {
      const metadata = this.validateForm()
      if (metadata) {
        const toUpdate = (this.single ? [this.series] : this.series) as SeriesDto[]
        for (const s of toUpdate) {
          try {
            await this.$komgaSeries.updateMetadata(s.id, metadata)
          } catch (e) {
            this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
          }
        }
        return true
      } else return false
    },
  },
})
</script>

<style lang="sass" scoped>
@import 'src/styles/tabbed-dialog'
</style>
