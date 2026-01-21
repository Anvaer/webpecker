<template>
  <!-- @change="() => $emit('update:selectedUrl', $event.target.value)" -->
  <Listbox
    :value="selectedUrl"
    @change="$emit('update:selectedUrl', $event.value)"
    :options="urlList"
    multiple
    optionLabel="name"
    optionValue="id"
    class="block m-2"
    scrollHeight="calc(100vh - 60px)"
  >
    <template #option="item">
      <div class="w-full">
        <div class="block text-left mb-2">
          {{ "#" + item.option.id + " " + item.option.name }}
        </div>
        <div class="flex status-bar">
          <div class="flex-1 text-left" style="line-height: 21px">
            {{ item.option.iteration }} / {{ item.option.total }}
          </div>
          <div class="flex-1 text-right font-weight-bold">
            {{ item.option.state }}
            <Button
              v-if="item.option.state === 'running'"
              class="ml-3 very-small-button"
              icon="pi pi-times"
              severity="danger"
              rounded
              aria-label="Stop"
              @click="
                (e) => {
                  $emit('stop-request-loop', item.option.id);
                  e.stopPropagation();
                }
              "
            />
          </div>
        </div>
        <div class="block">
          <ProgressBar
            :value="
              repeat > 0
                ? Math.floor((item.option.iteration * 100) / item.option.total)
                : 0
            "
            :pt="{
              value: () => ({
                style: {
                  backgroundColor: PROGRESS_BAR_COLORS[item.option.state],
                },
              }),
            }"
          />
        </div>
      </div>
    </template>
  </Listbox>
</template>
<script>
import { Listbox, ProgressBar } from "primevue";

export default {
  name: "UrlLoaderDialog",
  components: {
    Listbox,
    ProgressBar,
  },
  emit: ["update:selectedUrl", "stop-request-loop"],
  props: {
    urlList: {
      type: Array,
      default() {
        return [];
      },
    },
    selectedUrl: {
      type: Object,
      default() {
        return null;
      },
    },
  },

  setup() {
    const PROGRESS_BAR_COLORS = {
      "not-started": "#6c757d",
      running: "#ffc107",
      done: "#28a745",
      failed: "#dc3545",
      cancelled: "#6c757d",
    };
    return { PROGRESS_BAR_COLORS };
  },
};
</script>
