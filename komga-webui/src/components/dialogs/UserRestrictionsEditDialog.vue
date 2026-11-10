<template>
  <v-dialog v-model="modal"
            :fullscreen="$vuetify.breakpoint.xsOnly"
            max-width="600"
  >
    <v-card>
      <v-form v-model="form" ref="form">
        <v-toolbar class="hidden-sm-and-up">
          <v-btn icon @click="dialogCancel">
            <v-icon>mdi-close</v-icon>
          </v-btn>
          <v-toolbar-title>
            {{ $t('dialog.edit_user_restrictions.edit_restrictions_for', {name: user.email}) }}
          </v-toolbar-title>
          <v-spacer/>
          <v-toolbar-items>
            <v-btn text
                   color="primary"
                   :disabled="!form"
                   @click="dialogConfirm">{{ $t('common.save_changes') }}</v-btn>
          </v-toolbar-items>
        </v-toolbar>

        <v-card-title class="hidden-xs-only">
          {{ $t('dialog.edit_user_restrictions.edit_restrictions_for', {name: user.email}) }}
        </v-card-title>

        <!--  Fork note: content restrictions (age rating, sharing labels) have been
              removed along with the corresponding series metadata.  -->
        <v-container fluid class="pa-6">
          <v-row>
            <v-col>
              <v-checkbox v-model="allLibraries"
                          :label="$t('common.all_libraries')"
                          hide-details
                          class="my-0 py-0"
              />
            </v-col>
          </v-row>

          <v-divider class="my-2"/>

          <v-row v-for="(l, index) in libraries" :key="index">
            <v-col>
              <v-checkbox v-model="selectedLibraries"
                          :label="l.name"
                          :value="l.id"
                          :disabled="allLibraries"
                          hide-details
                          class="my-0 py-0"
              />
            </v-col>
          </v-row>
        </v-container>

        <v-card-actions class="hidden-xs-only">
          <v-spacer/>
          <v-btn text @click="dialogCancel">{{ $t('common.cancel') }}</v-btn>
          <v-btn color="primary"
                 @click="dialogConfirm"
                 :disabled="!form"
          >{{ $t('common.save_changes') }}
          </v-btn>
        </v-card-actions>
      </v-form>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import Vue from 'vue'
import {ERROR} from '@/types/events'
import {LibraryDto} from '@/types/komga-libraries'
import {UserDto, UserUpdateDto} from '@/types/komga-users'

export default Vue.extend({
  name: 'UserRestrictionsEditDialog',
  data: () => {
    return {
      form: false,
      modal: false,
      allLibraries: true,
      selectedLibraries: [] as string[],
    }
  },
  props: {
    value: Boolean,
    user: {
      type: Object,
      required: true,
    },
  },
  watch: {
    value(val) {
      this.modal = val
    },
    modal(val) {
      if (val) {
        // no-op
      } else {
        this.dialogCancel()
      }
    },
    user(val) {
      this.dialogReset(val)
    },
  },
  computed: {
    libraries(): LibraryDto[] {
      return this.$store.state.komgaLibraries.libraries
    },
  },
  methods: {
    dialogReset(user: UserDto) {
      (this.$refs.form as any)?.resetValidation()
      this.allLibraries = user.sharedAllLibraries
      if (user.sharedAllLibraries) {
        this.selectedLibraries = this.libraries.map(x => x.id)
      } else {
        this.selectedLibraries = user.sharedLibrariesIds
      }
    },
    dialogCancel() {
      this.$emit('input', false)
      this.dialogReset(this.user)
    },
    dialogConfirm() {
      this.editUser()
      this.$emit('input', false)
      this.dialogReset(this.user)
    },
    async editUser() {
      try {
        const patch = {
          sharedLibraries: {
            all: this.allLibraries,
            libraryIds: this.selectedLibraries,
          },
        } as UserUpdateDto

        await this.$store.dispatch('updateUser', {userId: this.user.id, patch: patch})
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
  },
})
</script>

<style scoped>

</style>
