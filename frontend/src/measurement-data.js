export const useMeasurementData = () => {
  // data =
  // {
  //  <url id>: {
  //    <okhttp3 event>: [
  //      [
  //        <absolute time in secs>,
  //        <relative time in ms>,
  //        <iteration number for url id>
  //      ]
  //    ]
  //  }
  // }
  const data = {};

  const idFilter = (id, selectedId) =>
    Array.isArray(selectedId)
      ? selectedId.length === 0 || selectedId.indexOf(parseInt(id)) !== -1
      : selectedId === null || parseInt(id) === parseInt(selectedId);
  // Agreggates data with given functions
  // Input:
  //  selectedId - selected ids to be aggregated for (null - for all)
  //  measurement - name of the measurement to be aggregated
  //  aggrFunc - functions for aggregation [func1, funct2]
  //  initVals - initial values for aggregate functions [initVal1, initVal2]
  // Output:
  //  array of aggregated objects with same order as input functions
  //  [
  //    {
  //      <absolute time 1>: <aggregated value 1>,
  //      <absolute time 2>: <aggregated value 2>,
  //      <absolute time 3>: <aggregated value 3>,
  //    }
  //    {
  //      <absolute time 1>: <aggregated value 1>,
  //      <absolute time 2>: <aggregated value 2>,
  //      <absolute time 3>: <aggregated value 3>,
  //    }
  //  ]
  const getAggregatedData = (
    selectedId,
    measurement,
    aggrFunc = [(v) => v + 1],
    initVals = [0],
  ) => {
    const res = aggrFunc.map(() => ({}));
    if (!Array.isArray(initVals)) initVals = aggrFunc.map(() => initVals);
    else if (initVals.length === 0) {
      initVals[0] = [];
    }
    if (initVals.length !== aggrFunc.length) {
      let lastVal = initVals[0];
      initVals = aggrFunc.map((f, i) => {
        initVals[i] = initVals[i] || lastVal;
        lastVal = initVals[i];
        return initVals[i];
      });
    }

    Object.entries(data)
      .filter(([id]) => idFilter(id, selectedId))
      .forEach(([, mes]) => {
        if (mes[measurement] !== undefined)
          mes[measurement].forEach((point) => {
            const t = point[0];
            aggrFunc.map((f, fi) => {
              res[fi][t] = res[fi][t] || structuredClone(initVals[fi]);
              res[fi][t] = f(res[fi][t], point[1]);
            });
          });
      });
    return res;
  };

  const getStageDuarations = (selectedId, stage) => {
    const res = [];
    Object.entries(data)
      .filter(([id]) => idFilter(id, selectedId))
      .forEach(([, mes]) => {
        if (mes[stage + "End"] !== undefined) {
          const stageEndList = [
            ...mes[stage + "End"].map((i) => [...i, 0]),
            ...(mes[stage + "Failed"]?.map((i) => [...i, 1]) ?? []),
          ].sort((a, b) => {
            const iterSort = a[2] - b[2];
            if (iterSort === 0) return a[1] - b[1];
            else return iterSort;
          });

          let j = 0;
          mes[stage + "Start"].forEach((val) => {
            // searching for same iteration number
            while (stageEndList[j] && val[2] > stageEndList[j][2]) j++;
            if (!stageEndList[j]) return;
            // gathering only successful calls
            if (stageEndList[j][3] === 0 && val[1] < stageEndList[j][1]) {
              res.push(stageEndList[j][1] - val[1]);
            }
            j++;
          });
        }
      });
    return res;
  };

  const getTableData = (urlList) => {
    const stats = Object.keys(data).map((id) => {
      const [respTimes] = getAggregatedData(
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
      const responseTimesData = Object.values(respTimes).flatMap((i) => i);

      const [failedCalls] = getAggregatedData(
        id,
        "callFailed",
        [
          (a, v) => {
            a.push(v);
            return a;
          },
        ],
        [],
      );

      const failedCallsData = Object.values(failedCalls).flatMap((i) => i);

      const median = (values) => {
        if (values.length === 0) return;
        values = [...values].sort((a, b) => a - b);
        const half = Math.floor(values.length / 2);
        return values.length % 2
          ? values[half]
          : (values[half - 1] + values[half]) / 2;
      };

      const trimmedMean = (values, trimPercent) => {
        if (values.length === 0) return;
        const sortedValues = values.sort();
        const trimCount = Math.floor(
          sortedValues.length * ((trimPercent / 2) * 0.01),
        );
        const trimmedValues = sortedValues.slice(
          trimCount,
          sortedValues.length - trimCount,
        );
        if (!trimmedValues.length) return null;
        const sum = trimmedValues.reduce((acc, value) => acc + value, 0);
        return Math.round(sum / trimmedValues.length);
      };

      return {
        id,
        url: urlList[parseInt(id)],
        count: responseTimesData.length,
        countError: failedCallsData.length,
        mean:
          responseTimesData.length > 0
            ? Math.round(
                responseTimesData.reduce((a, c) => a + c, 0) /
                  responseTimesData.length,
              )
            : undefined,
        median: median(responseTimesData),
        trimmedMean: trimmedMean(responseTimesData, 10),
      };
    });
    return stats;
  };

  // input point =
  // {
  //  id: <url id>,
  //  event:<okhttp3 event>,
  //  time:<event absolute time in ms>,
  //  msFromStart:<event relative time in ms>,
  //  iteration:<iteration number for url id>
  // }
  const addMeasurementPoint = (point) => {
    data[point.id] = data[point.id] || {};
    data[point.id][point.event] = data[point.id][point.event] || [];
    data[point.id][point.event].push([
      Math.floor(point.time / 1000),
      point.msFromStart,
      point.iteration,
    ]);
  };

  const clearData = () => {
    Object.keys(data).forEach((key) => {
      delete data[key];
    });
  };

  return {
    addMeasurementPoint,
    getAggregatedData,
    getStageDuarations,
    getTableData,
    clearData,
  };
};
