<template>
  <Dialog
    :visible="viewDialog"
    header="URL List"
    base-z-index="9999"
    position="center"
    :modal="true"
    :draggable="false"
    :dismissable-mask="true"
    @update:visible="() => $emit('update:viewDialog', false)"
  >
    <Message class="mb-3" severity="warn">
      Maximum 100 valid URLs are allowed
    </Message>
    <div class="flex items-center gap-2 mb-3">
      <Textarea
        v-model="urlListPlain"
        rows="10"
        cols="100"
        placeholder="Enter URLs, one per line"
        style="width: 100%"
      ></Textarea>
    </div>
    <div class="flex">
      <div class="flex-1 justify-center gap-2">
        <Button
          type="button"
          label="Save"
          class="mr-2"
          @click="
            () => {
              $emit('load-url-list', urlListPlain);
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
      <div class="flex">
        <FileUpload
          mode="basic"
          chooseLabel="Load from file"
          @select="uploadUrls"
          customUpload
          auto
          severity="secondary"
          class="p-button-outlined"
        />
      </div>
    </div>
  </Dialog>
</template>
<script>
import { Dialog, Message, Button, FileUpload, Textarea } from "primevue";
import { ref } from "vue";

export default {
  name: "UrlLoaderDialog",
  components: { Dialog, Message, Button, FileUpload, Textarea },
  emits: ["update:viewDialog", "load-url-list"],
  props: {
    viewDialog: {
      type: Boolean,
      default() {
        return false;
      },
    },
  },
  setup() {
    const urlListPlain = ref(
      `https://google.com
https://www.bing.com/`,
    );

    const uploadUrls = (ev) => {
      const file = ev.files[0];
      const reader = new FileReader();

      reader.onload = async (e) => {
        urlListPlain.value = e.target.result;
      };

      reader.readAsText(file);
    };

    return {
      urlListPlain,
      uploadUrls,
    };
  },
};
</script>
