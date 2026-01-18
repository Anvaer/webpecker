import * as echarts from "echarts";

export const useStagesChart = (htmlContainerId, measurementData) => {
  let stagesChart = null;

  const monitoredStages = [
    "proxySelect",
    "dns",
    "connect",
    "secureConnect",
    "TTFB",
    "responseHeaders",
    "responseBody",
  ];

  const stagesColors = [
    "#9E9E9E",
    "#4CAF50",
    "#FF9800",
    "#F44336",
    "#364cba",
    "#ab4ac3",
    "#8BC34A",
  ];

  const init = () => {
    const chartDom = document.getElementById(htmlContainerId);
    stagesChart = echarts.init(chartDom);

    const option = {
      title: {
        text: "Response Time Stages",
      },
      toolbox: {
        show: true,
        feature: {
          dataView: { readOnly: false },
          saveAsImage: {},
        },
      },
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "shadow",
        },
        formatter: function (params) {
          const tar = params[1];
          return (
            tar.axisValue +
            " : " +
            tar.value +
            " ms<br/>[" +
            tar.data.name +
            " calls]"
          );
        },
      },
      dataZoom: {
        show: false,
        start: 0,
        end: 100,
      },
      xAxis: [
        {
          type: "value",
          name: "Response time, ms",
          nameLocation: "middle",
        },
      ],
      yAxis: [
        {
          type: "category",
          inverse: true,
          splitLine: { show: false },
          name: "Stages",
          nameLocation: "middle",
          data: monitoredStages,
        },
      ],
      series: [
        {
          emphasis: {
            itemStyle: {
              borderColor: "transparent",
              color: "transparent",
            },
          },
          itemStyle: {
            borderColor: "transparent",
            color: "transparent",
          },
          type: "bar",
          stack: "stages",
        },
        {
          name: "Stage",
          type: "bar",
          stack: "stages",
        },
      ],
    };

    option && stagesChart.setOption(option);

    const resizeObserver = new ResizeObserver(() => {
      stagesChart.resize();
    });
    resizeObserver.observe(chartDom);
  };

  const updateData = (selectedId) => {
    const monitoredStagesData = {};
    const monitoredStagesDataCount = {};

    monitoredStages.forEach((stage) => {
      let stageSum = 0;
      let stageCount = 0;

      if (stage === "TTFB") {
        const [ttfb] = measurementData.getAggregatedData(
          selectedId,
          "responseHeadersStart",
          [
            (a, v) => {
              a.push(v);
              return a;
            },
          ],
          []
        );
        const ttfbFlat = Object.values(ttfb).flatMap((i) => i);
        stageSum = ttfbFlat.reduce((a, c) => a + c, 0);
        stageCount = ttfbFlat.length;
      } else {
        const durs = measurementData.getStageDuarations(selectedId, stage);
        stageSum = durs.reduce((a, c) => a + c, 0);
        stageCount = durs.length;
      }
      monitoredStagesData[stage] =
        stageCount > 0 ? Math.round(stageSum / stageCount) : 0;
      monitoredStagesDataCount[stage] = stageCount;
    });

    const stageDur = monitoredStages.map((s) => monitoredStagesData[s] ?? 0);

    /*
    0 proxySelect        |==
    1 dns                |  ====
    2 connect            |      ======
    3 secureConnect      |         ===
    4 TTFB               |=================
    5 responseHeaders    |                 ====
    6 responseBody       |                     ====
    */

    const indent = [];

    indent[0] = 0;
    indent[1] = stageDur[0];
    indent[2] = indent[1] + stageDur[1];
    indent[3] = indent[2] + stageDur[2] - stageDur[3];
    indent[4] = 0;
    indent[5] = stageDur[4];
    indent[6] = indent[5] + stageDur[5];

    stagesChart.setOption({
      series: [
        {
          data: indent,
        },
        {
          data: monitoredStages.map((s, i) => ({
            name: monitoredStagesDataCount[s] ?? 0,
            value: monitoredStagesData[s] ?? 0,
            itemStyle: { color: stagesColors[i] },
          })),
        },
      ],
    });
  };

  return { init, updateData };
};
