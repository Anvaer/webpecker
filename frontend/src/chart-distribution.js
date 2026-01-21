import * as echarts from "echarts";

export const useDistributionChart = (htmlContainerId, measurementData) => {
  let distrChart = null;

  const smartDecimal = (num, bucketSize) => {
    if (num < 0.2 || bucketSize < 0.2) {
      return num.toFixed(2);
    } else if (num < 5 || bucketSize < 5) {
      return num.toFixed(1);
    } else {
      return Math.round(num).toString();
    }
  };

  // {id: {measuremnet:[[time,val,iter]]}}

  const init = () => {
    const chartDom = document.getElementById(htmlContainerId);
    distrChart = echarts.init(chartDom);

    const option = {
      title: {
        text: "Response Time Distribution",
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
          type: "cross",
          label: {
            backgroundColor: "#283b56",
          },
        },
      },
      dataZoom: {
        show: false,
        start: 0,
        end: 100,
      },
      xAxis: [
        {
          type: "category",
          name: "Response time buckets, ms",
          nameLocation: "middle",
        },
      ],
      yAxis: [
        {
          type: "value",
          scale: true,
          name: "Count",
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
          name: "Total",
        },
      ],
    };

    option && distrChart.setOption(option);

    const resizeObserver = new ResizeObserver(() => {
      distrChart.resize();
    });
    resizeObserver.observe(chartDom);
  };

  const bucketsArray = {};
  const responseTimesData = {};
  const bucketNamesArray = [];

  const lastSeenIds = [];

  const updateData = (selectedUrl) => {
    if (!Array.isArray(selectedUrl)) selectedUrl = [selectedUrl];
    if (selectedUrl.length === 0) selectedUrl = [null];

    Object.keys(responseTimesData).forEach(
      (id) => delete responseTimesData[id],
    );
    Object.keys(bucketsArray).forEach((id) => delete bucketsArray[id]);

    selectedUrl.forEach((id) => {
      const [respTimes] = measurementData.getAggregatedData(
        id,
        "callEnd",
        [
          (a, v) => {
            a.push(v);
            return a;
          },
        ],
        [],
      );

      responseTimesData[id] = Object.values(respTimes).flatMap((i) => i);
      bucketsArray[id] = bucketsArray[id] || [];
      bucketsArray[id].splice(0);
    });

    const allResponseTimesData = Object.values(responseTimesData).flatMap(
      (i) => i,
    );

    const intervals = [];
    const min = allResponseTimesData.reduce((a, b) => Math.min(a, b), Infinity);
    const max = allResponseTimesData.reduce(
      (a, b) => Math.max(a, b),
      -Infinity,
    );
    const responseTimeDataMaxLength = Object.values(responseTimesData).reduce(
      (a, c) => (c.length > a ? c.length : a),
      -Infinity,
    );
    const bucketCount = Math.ceil(Math.sqrt(responseTimeDataMaxLength));
    const bucketSize = (max - min) / bucketCount;
    bucketNamesArray.splice(0);

    for (let i = 0; i < bucketCount; i++) {
      Object.keys(bucketsArray).forEach((id) => bucketsArray[id].push(0));
      const rangeStart = min + i * bucketSize;
      const rangeEnd = min + (i + 1) * bucketSize;
      intervals.push([min + i * bucketCount, min + (i + 1) * bucketSize]);
      bucketNamesArray.push(
        `${smartDecimal(rangeStart, bucketSize)} - ${smartDecimal(
          rangeEnd,
          bucketSize,
        )}`,
      );
    }
    Object.keys(responseTimesData).forEach((id) =>
      responseTimesData[id].forEach((value) => {
        const index = Math.min(
          Math.floor((value - min) / bucketSize),
          bucketCount - 1,
        );
        bucketsArray[id][index]++;
      }),
    );

    distrChart.setOption(
      {
        animation: false,
        xAxis: [
          {
            data: bucketNamesArray,
          },
        ],
        series: Object.keys(bucketsArray).map((id) => ({
          name: id === null ? "Total" : id,
          type: "line",
          smooth: true,
          lineStyle: {
            width: 0,
          },
          showSymbol: false,
          areaStyle: {
            opacity: 0.6,
          },
          emphasis: {
            focus: "series",
          },
          data: bucketsArray[id],
        })),
      },
      JSON.stringify(lastSeenIds) !== JSON.stringify(Object.keys(bucketsArray))
        ? {
            replaceMerge: ["series"],
          }
        : undefined,
    );

    lastSeenIds.splice(0);
    lastSeenIds.push(...Object.keys(bucketsArray));
  };

  return { init, updateData };
};
