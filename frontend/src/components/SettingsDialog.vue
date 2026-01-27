<template>
  <Dialog
    :visible="viewDialog"
    header="Settings"
    base-z-index="9999"
    position="center"
    :modal="true"
    :draggable="false"
    :dismissable-mask="true"
    @update:visible="() => $emit('update:viewDialog', false)"
  >
    <div class="gap-2 mb-3">
      <InputGroup class="mb-3">
        <InputGroupAddon class="input-group-label">
          Number of iterations for each URL
          <Message v-if="testIsRunning" severity="warn">
            Unable to change while measurement is running
          </Message>
        </InputGroupAddon>
        <InputNumber v-model="localSettings.repeat" :disabled="testIsRunning" />
      </InputGroup>
      <InputGroup class="mb-3">
        <InputGroupAddon class="input-group-label">
          Delay between requests
        </InputGroupAddon>
        <InputNumber v-model="localSettings.delay" suffix=" ms" />
      </InputGroup>
      <InputGroup class="mb-3">
        <InputGroupAddon class="input-group-label"> Timeout </InputGroupAddon>
        <InputNumber v-model="localSettings.timeout" suffix=" ms" />
      </InputGroup>
      <InputGroup>
        <InputGroupAddon class="input-group-label">
          Max number of concurrent requests
        </InputGroupAddon>
        <InputNumber v-model="localSettings.maxConcurrent" />
      </InputGroup>
    </div>
    <div class="flex">
      <div class="flex-1 justify-center gap-2">
        <Button
          type="button"
          label="Save"
          class="mr-2"
          @click="
            () => {
              $emit('update:settings', { ...localSettings });
              $emit('update:viewDialog', false);
            }
          "
        ></Button>
        <Button
          type="button"
          label="Cancel"
          severity="secondary"
          @click="() => $emit('update:viewDialog', false)"
        ></Button>
      </div>
    </div>
  </Dialog>
</template>
<script>
import {
  Dialog,
  Message,
  InputGroup,
  InputGroupAddon,
  InputNumber,
} from "primevue";
import { ref, watch, toRefs } from "vue";

export default {
  name: "SettingsDialog",
  components: { Dialog, Message, InputGroup, InputGroupAddon, InputNumber },
  emits: ["update:viewDialog", "update:settings"],
  props: {
    viewDialog: {
      type: Boolean,
      default() {
        return false;
      },
    },
    settings: {
      type: Object,
      default() {
        return {};
      },
    },
    testIsRunning: {
      type: Boolean,
      default() {
        return false;
      },
    },
  },
  setup(props) {
    const localSettings = ref(props.settings);
    const settings = toRefs(props).settings;

    watch(
      settings,
      (newSettings) => (localSettings.value = { ...newSettings }),
    );

    return { localSettings };
  },
};
</script>
