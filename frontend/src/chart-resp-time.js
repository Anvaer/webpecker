import * as echarts from "echarts";

export const useReposneTimeChart = (htmlContainerId, measurementData) => {
  let rtChart = null;

  const successCallsData = [];
  const failedCallsData = [];
  const responseTimeData = [];

  const init = () => {
    const chartDom = document.getElementById(htmlContainerId);
    rtChart = echarts.init(chartDom);

    const option = {
      title: {
        text: "Throughput & Response Time",
      },
      tooltip: {
        trigger: "axis",
        axisPointer: {
          type: "cross",
          label: {
            backgroundColor: "#283b56",
          },
        },
      },
      legend: { symbol: "none" },
      toolbox: {
        show: true,
        feature: {
          dataView: { readOnly: false },
          saveAsImage: {},
        },
      },
      dataZoom: {
        show: false,
        start: 0,
        end: 100,
      },
      xAxis: [
        {
          type: "time",
          boundaryGap: true,
        },
      ],
      yAxis: [
        {
          type: "value",
          scale: true,
          name: "Throughput",
          nameLocation: "middle",
          splitLine: {
            show: false,
          },
          min: 0,
          boundaryGap: [0.2, 0.2],
        },
        {
          type: "value",
          scale: true,
          name: "Response time, ms",
          nameLocation: "middle",
          splitLine: {
            show: false,
          },
          min: 0,
          boundaryGap: [0.2, 0.2],
        },
      ],
      series: [
        {
          name: "Successful calls",
          type: "bar",
          itemStyle: {
            color: "#28a745",
          },
          stack: "throughput",
          data: successCallsData,
        },
        {
          name: "Failed calls",
          type: "bar",
          itemStyle: {
            color: "#dc3545",
          },
          stack: "throughput",
          data: failedCallsData,
        },
        {
          name: "Response time",
          type: "line",
          symbol: "none",
          yAxisIndex: 1,
          data: responseTimeData,
        },
      ],
    };

    option && rtChart.setOption(option);

    const resizeObserver = new ResizeObserver(() => {
      rtChart.resize();
    });
    resizeObserver.observe(chartDom);
  };

  const updateData = (selectedUrl) => {
    const [successCalls, rtSum] = measurementData.getAggregatedData(
      selectedUrl,
      "callEnd",
      [(a) => a + 1, (a, v) => a + v]
    );

    const [failedCalls] = measurementData.getAggregatedData(
      selectedUrl,
      "callFailed",
      [(a) => a + 1]
    );

    successCallsData.splice(0);
    failedCallsData.splice(0);
    responseTimeData.splice(0);
    [...new Set([successCalls, failedCalls].flatMap(Object.keys))].map((t) => {
      const ti = new Date(t * 1000);
      successCallsData.push([ti, successCalls[t] || 0]);
      failedCallsData.push([ti, failedCalls[t] || 0]);
      if (successCalls[t] > 0)
        responseTimeData.push([ti, Math.round(rtSum[t] / successCalls[t])]);
    });

    rtChart.setOption({
      series: [
        {
          data: successCallsData,
        },
        {
          data: failedCallsData,
        },
        {
          data: responseTimeData,
        },
      ],
    });
  };

  return { init, updateData };
};
