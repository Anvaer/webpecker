<template>
  <div class="app-wrapper">
    <Splitter style="height: 100%">
      <SplitterPanel class="flex items-center justify-center" :size="15">
        <div class="w-full">
          <div class="flex text-left mx-2 mt-2">
            <div class="flex-1">
              <UrlLoaderDialog
                @loadUrlList="
                  (urlListPlain) => {
                    clear();
                    urlList = urlListPlain
                      .split('\n')
                      .map((u) => u.trim())
                      .filter((u) => u.length > 0)
                      .filter((u) => isValidUrl(u))
                      .slice(0, 100);
                  }
                "
              />
              <Button
                class="mr-2"
                label="Start"
                severity="success"
                :disabled="urlOptions.length === 0 || testIsRunning"
                @click="() => execRequests()"
              />
              <Button
                class="mr-2"
                label="Stop"
                severity="danger"
                :disabled="urlOptions.length === 0 || !testIsRunning"
                @click="stop()"
              />
              <Button
                class="mr-2"
                label="Clear"
                severity="warn"
                :disabled="urlOptions.length === 0 || testIsRunning"
                @click="clear()"
              />
              <Button
                class="mr-2"
                label="Reset HTTP client"
                severity="warn"
                @click="
                  () =>
                    sendMessage(JSON.stringify({ action: 'reset-http-client' }))
                "
              />
            </div>
            <div class="flex">
              <Button
                icon="pi pi-cog"
                severity="info"
                @click="showSettings = true"
              />
              <Dialog
                v-model:visible="showSettings"
                header="Settings"
                base-z-index="9999"
                position="center"
                :modal="true"
                :draggable="false"
                :dismissable-mask="true"
              >
                <InputGroup class="mb-3">
                  <InputGroupAddon class="input-group-label">
                    Number of iterations for each URL
                    <Message v-if="testIsRunning" severity="warn">
                      Unable to change while measurement is running
                    </Message>
                  </InputGroupAddon>
                  <InputNumber v-model="repeat" :disabled="testIsRunning" />
                </InputGroup>
                <InputGroup class="mb-3">
                  <InputGroupAddon class="input-group-label">
                    Delay between requests
                  </InputGroupAddon>
                  <InputNumber v-model="delay" suffix=" ms" />
                </InputGroup>
                <InputGroup class="mb-3">
                  <InputGroupAddon class="input-group-label">
                    Timeout
                  </InputGroupAddon>
                  <InputNumber v-model="timeout" suffix=" ms" />
                </InputGroup>
                <InputGroup>
                  <InputGroupAddon class="input-group-label">
                    Max number of concurrent requests
                  </InputGroupAddon>
                  <InputNumber v-model="maxConcurrent" />
                </InputGroup>
              </Dialog>
            </div>
          </div>
          <Listbox
            v-model="selectedUrl"
            :options="urlOptions"
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
                    {{ item.option.reqDone }} / {{ item.option.reqTotal }}
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
                          stop(item.option.id);
                          e.stopPropagation();
                        }
                      "
                    />
                  </div>
                </div>
                <div class="block">
                  <ProgressBar
                    :value="
                      item.option.reqTotal > 0
                        ? Math.floor(
                            (item.option.reqDone * 100) / item.option.reqTotal
                          )
                        : 0
                    "
                    :pt="{
                      value: () => ({
                        style: {
                          backgroundColor:
                            PROGRESS_BAR_COLORS[item.option.state],
                        },
                      }),
                    }"
                  />
                </div>
              </div>
            </template>
          </Listbox>
        </div>
      </SplitterPanel>
      <SplitterPanel class="flex items-center justify-center">
        <Splitter style="height: 100%" layout="vertical">
          <SplitterPanel
            class="flex items-center justify-center"
            style="height: 360px"
          >
            <div id="response-time-chart" style="width: 30%; height: 100%" />
            <div id="distribution-chart" style="width: 30%; height: 100%" />
            <div id="stages-chart" style="width: 40%; height: 100%" />
          </SplitterPanel>
          <SplitterPanel class="flex items-center justify-center">
            <DataTable
              :value="tableData"
              :data-key="id"
              :scrollable="true"
              scroll-height="100%"
              class="flex-1"
            >
              <Column field="id" header="#" sortable></Column>
              <Column field="url" header="URL" sortable>
                <template #body="props">
                  <span>
                    {{
                      urlOptions.find(
                        (u) => parseInt(u.id) === parseInt(props.data.id)
                      )?.value
                    }}
                  </span>
                </template>
              </Column>
              <Column field="count" header="Successful" sortable></Column>
              <Column field="countError" header="Failed" sortable></Column>
              <Column field="mean" header="Mean" sortable></Column>
              <Column field="median" header="Median" sortable></Column>
              <Column
                field="trimmedMean"
                header="Trimmed Mean"
                sortable
              ></Column
            ></DataTable>
          </SplitterPanel>
        </Splitter>
      </SplitterPanel>
    </Splitter>
  </div>
</template>

<script>
import {
  Button,
  Listbox,
  SplitterPanel,
  Splitter,
  ProgressBar,
  InputNumber,
  InputGroup,
  InputGroupAddon,
  Message,
  DataTable,
  Column,
  Dialog,
} from "primevue";
import { computed, ref, watch, onMounted } from "vue";
import { useWebSocket } from "./websocket.api";
import { useMeasurementData } from "./measurement-data";
import { useReposneTimeChart } from "./chart-resp-time";
import { useDistributionChart } from "./chart-distribution";
import { useStagesChart } from "./chart-stages";
import UrlLoaderDialog from "./components/UrlLoaderDialog.vue";

export default {
  name: "App",
  components: {
    Button,
    Listbox,
    SplitterPanel,
    Splitter,
    ProgressBar,
    Dialog,
    InputNumber,
    InputGroup,
    InputGroupAddon,
    Message,
    DataTable,
    Column,
    UrlLoaderDialog,
  },
  setup() {
    const UPDATE_INTERVAL = 1000;
    const PROGRESS_BAR_COLORS = {
      "not-started": "#6c757d",
      running: "#ffc107",
      done: "#28a745",
      failed: "#dc3545",
      cancelled: "#6c757d",
    };

    const showSettings = ref(false);

    const urlList = ref([]);

    const repeat = ref(1000);
    const delay = ref(10);
    const timeout = ref(600);
    const maxConcurrent = ref(3);

    const selectedUrl = ref(null);
    const state = ref({});
    const iteration = ref({});
    const tableData = ref([]);

    let intervalId = null;

    const { sendMessage } = useWebSocket(`ws://${window.location.host}/req`, {
      onMessage: (m) => msgListner(m),
      onOpen: () => {
        sendMessage(
          JSON.stringify({
            action: "update-config",
            maxConcurrent: maxConcurrent.value,
            timeout: timeout.value,
            delay: delay.value,
          })
        );
      },
    });

    // event data
    const measurementData = useMeasurementData();
    const msgListner = (m) => {
      const point = JSON.parse(m.data);
      if (point.state) state.value[point.id] = point.state;
      if (point.iteration) iteration.value[point.id] = point.iteration;
      if (point.event) {
        measurementData.addMeasurementPoint(point);
      }
    };

    // charts
    const rtChart = useReposneTimeChart("response-time-chart", measurementData);
    const distrChart = useDistributionChart(
      "distribution-chart",
      measurementData
    );
    const stagesChart = useStagesChart("stages-chart", measurementData);
    onMounted(() => {
      rtChart.init();
      distrChart.init();
      stagesChart.init();
    });
    const updateCharts = (urdId = undefined) => {
      rtChart.updateData(urdId);
      distrChart.updateData(urdId);
      stagesChart.updateData(urdId);
    };
    watch(selectedUrl, (newVal) => {
      updateCharts(newVal);
    });

    const isValidUrl = (u) => {
      try {
        new URL(u);
        return true;
      } catch (e) {
        console.error("not valid URL: ", e, u);
        return false;
      }
    };

    watch(
      urlList,
      (newList) => {
        newList.forEach((u, id) => (state.value[id] = "not started"));
      },
      { immediate: true }
    );

    const urlOptions = computed(() =>
      urlList.value.map((url, id) => ({
        id,
        name: url,
        value: url,
        state: state.value[id],
        reqDone: iteration.value[id] ?? 0,
        reqTotal: repeat.value,
      }))
    );

    const urlRequests = computed(() =>
      urlOptions.value.map((item, id) => ({
        id,
        url: item.value,
        repeat: repeat.value,
        action: "send-request",
      }))
    );

    watch([delay, maxConcurrent, timeout], ([d, mc, t]) => {
      sendMessage(
        JSON.stringify({
          action: "update-config",
          delay: d,
          maxConcurrent: mc,
          timeout: t,
        })
      );
    });

    // actions
    const execRequests = () => {
      clear();
      urlRequests.value.forEach((req) => {
        sendMessage(JSON.stringify(req));
      });
    };

    const stop = (id) => {
      sendMessage(JSON.stringify({ action: "cancel-request", id }));
    };

    const clear = () => {
      urlList.value.forEach((u, id) => (state.value[id] = "not started"));
      Object.keys(iteration.value).forEach((k) => (iteration.value[k] = 0));
      measurementData.clearData();
      updateCharts();
    };

    const testIsRunning = computed(() => {
      return Object.values(state.value).some((s) => s === "running");
    });

    const updateAll = () => {
      rtChart.updateData(selectedUrl.value);
      distrChart.updateData(selectedUrl.value);
      stagesChart.updateData(selectedUrl.value);
      tableData.value = measurementData.getTableData(
        urlOptions.value.map((o) => o.value)
      );
    };

    watch(testIsRunning, (isRunning) => {
      if (!isRunning && intervalId) {
        clearInterval(intervalId);
        updateAll();
        intervalId = null;
      } else if (isRunning)
        intervalId = setInterval(() => {
          console.log("updating vals");
          updateAll();
        }, UPDATE_INTERVAL);
    });

    return {
      PROGRESS_BAR_COLORS,
      showSettings,
      delay,
      repeat,
      timeout,
      tableData,
      maxConcurrent,
      state,
      isValidUrl,
      urlList,
      urlOptions,
      selectedUrl,
      testIsRunning,
      stop,
      clear,
      execRequests,
      sendMessage,
    };
  },
};
</script>

<style>
@font-face {
  font-family: "Rubik Light";
  src: url("./../public/fonts/Rubik-Light.ttf");
}
@font-face {
  font-family: "Rubik";
  src: url("./../public/fonts/Rubik-Regular.ttf");
}

html,
body {
  height: 100%;
  margin: 0;
  font-family: "Rubik";
}

.app-wrapper {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.p-splitter {
  height: 100%;
}

.p-splitter-panel {
  height: 100%;
  overflow: auto;
}

.panel-content {
  padding: 10px;
}

.status-bar {
  font-size: small;
}
.input-group-label {
  width: 350px;
  text-align: right;
  display: block !important;
}
.very-small-button {
  width: 21px !important;
  height: 21px !important;
}
.very-small-button .p-button-icon {
  font-size: 10.5px !important;
}
</style>
