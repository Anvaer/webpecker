<template>
  <div class="app-wrapper">
    <Splitter style="height: 100%">
      <SplitterPanel class="flex items-center justify-center" :size="15">
        <div class="w-full">
          <div class="flex text-left mx-2 mt-2">
            <div class="flex-1">
              <Button
                class="mr-2"
                label="Load URLs"
                severity="primary"
                @click="viewUrlLoaderDialog = !viewUrlLoaderDialog"
              />
              <UrlLoaderDialog
                v-model:viewDialog="viewUrlLoaderDialog"
                @load-url-list="
                  (urlListPlain) => {
                    stopRequestLoop();
                    clear();
                    buildUrlList(urlListPlain);
                    resetUrlListState();
                  }
                "
              />
              <Button
                class="mr-2"
                label="Start"
                severity="success"
                :disabled="urlList.length === 0 || testIsRunning"
                @click="
                  () => {
                    clear();
                    execRequests();
                  }
                "
              />
              <Button
                class="mr-2"
                label="Stop"
                severity="danger"
                :disabled="urlList.length === 0 || !testIsRunning"
                @click="stopRequestLoop()"
              />
              <Button
                class="mr-2"
                label="Clear"
                severity="warn"
                :disabled="urlList.length === 0 || testIsRunning"
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
              <SettingsDialog
                v-model:viewDialog="showSettings"
                v-model:settings="settings"
                :test-is-running="testIsRunning"
              />
            </div>
          </div>
          <UrlList
            v-model:selectedUrl="selectedUrl"
            :urlList="urlList"
            @stop-request-loop="(id) => stopRequestLoop(id)"
          />
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
            <ResultsTable :tableData="tableData" :urlList="urlList" />
          </SplitterPanel>
        </Splitter>
      </SplitterPanel>
    </Splitter>
  </div>
</template>

<script>
import { Button, SplitterPanel, Splitter } from "primevue";
import { computed, ref, watch, onMounted } from "vue";
import { useWebSocket } from "./websocket.api";
import { useMeasurementData } from "./measurement-data";
import { useReposneTimeChart } from "./chart-resp-time";
import { useDistributionChart } from "./chart-distribution";
import { useStagesChart } from "./chart-stages";
import UrlLoaderDialog from "./components/UrlLoaderDialog.vue";
import ResultsTable from "./components/ResultsTable.vue";
import UrlList from "./components/UrlList.vue";
import SettingsDialog from "./components/SettingsDialog.vue";

export default {
  name: "App",
  components: {
    Button,
    SplitterPanel,
    Splitter,
    UrlLoaderDialog,
    ResultsTable,
    UrlList,
    SettingsDialog,
  },
  setup() {
    const UPDATE_INTERVAL_MS = 25;

    const viewUrlLoaderDialog = ref(false);
    const showSettings = ref(false);

    const urlList = ref([]);

    const settings = ref({});

    const selectedUrl = ref(null);
    const tableData = ref([]);

    let intervalId = null;

    const { sendMessage } = useWebSocket(`ws://${window.location.host}/req`, {
      onMessage: (m) => msgListner(m),
      onOpen: () => {
        sendMessage(
          JSON.stringify({
            action: "restore-state",
          }),
        );
      },
    });

    // event data
    const measurementData = useMeasurementData();
    const msgListner = (m) => {
      const msgs = JSON.parse(m.data);
      for (let msg of msgs) {
        if (msg.maxConcurrent) restoreSettings(msg);
        else if (Array.isArray(msg)) restoreUrlListState(msg);
        else if (urlList.value?.length > 0) {
          if (msg.state) {
            const changedUrl = urlList.value.find((url) => url.id === msg.id);
            if (changedUrl) changedUrl.state = msg.state;
          } else if (msg.event) measurementData.addMeasurementPoint(msg);
          else if (msg.iteration)
            urlList.value[msg.id].iteration = msg.iteration;
        }
      }
    };

    // charts
    const rtChart = useReposneTimeChart("response-time-chart", measurementData);
    const distrChart = useDistributionChart(
      "distribution-chart",
      measurementData,
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

    // list building
    const isValidUrl = (u) => {
      try {
        new URL(u);
        return true;
      } catch (e) {
        console.error("not valid URL: ", e, u);
        return false;
      }
    };

    const buildUrlList = (urlListPlain) => {
      const list = urlListPlain
        .split("\n")
        .map((u) => u.trim())
        .filter((u) => u.length > 0)
        .filter((u) => isValidUrl(u))
        .slice(0, 100);
      urlList.value.splice(0);
      urlList.value.push(
        ...list.map((url, id) => ({
          id,
          name: url,
          value: url,
        })),
      );
    };

    const restoreSettings = (newSettings) =>
      (settings.value = { ...newSettings });

    const restoreUrlListState = (stateList) => {
      console.log(stateList);
      urlList.value.splice(0);
      urlList.value.push(
        ...stateList.map((url) => ({
          id: url.id,
          name: url.url,
          value: url.url,
          state: url.state,
          iteration: url.iteration,
          total: url.repeat,
        })),
      );
    };

    const resetUrlListState = () => {
      urlList.value.forEach((url) => {
        url.state = "not started";
        url.iteration = 0;
        url.total = settings.value.repeat;
      });
    };

    const buildRequests = () =>
      urlList.value.map((url, id) => ({
        id,
        url: url.value,
        repeat: settings.value.repeat,
        action: "send-request",
      }));

    watch(settings, (newS) => {
      console.log(newS);
      sendMessage(
        JSON.stringify({
          action: "update-config",
          delay: newS.delay,
          maxConcurrent: newS.maxConcurrent,
          timeout: newS.timeout,
        }),
      );
    });

    // actions
    const execRequests = () => {
      buildRequests(urlList.value).forEach((req) => {
        sendMessage(JSON.stringify(req));
      });
    };

    const stopRequestLoop = (id) => {
      sendMessage(JSON.stringify({ action: "cancel-request", id }));
    };

    const clear = () => {
      resetUrlListState();
      measurementData.clearData();
      updateCharts();
    };

    const testIsRunning = computed(() => {
      return urlList.value.some((url) => url.state === "running");
    });

    const updateAll = () => {
      rtChart.updateData(selectedUrl.value);
      distrChart.updateData(selectedUrl.value);
      stagesChart.updateData(selectedUrl.value);
      tableData.value = measurementData
        .getTableData(urlList.value.map((o) => o.value))
        .map((url) => ({
          ...url,
          url: urlList.value.find((u) => parseInt(u.id) === parseInt(url.id))
            ?.value,
        }));
    };

    watch(testIsRunning, (isRunning) => {
      if (!isRunning && intervalId) {
        clearInterval(intervalId);
        updateAll();
        intervalId = null;
      } else if (isRunning)
        intervalId = setInterval(() => {
          updateAll();
        }, UPDATE_INTERVAL_MS);
    });

    return {
      viewUrlLoaderDialog,
      showSettings,
      settings,
      tableData,
      isValidUrl,
      urlList,
      buildUrlList,
      resetUrlListState,
      selectedUrl,
      testIsRunning,
      stopRequestLoop,
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
