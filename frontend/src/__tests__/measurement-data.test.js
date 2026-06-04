import { describe, expect, it } from "vitest";
import { useMeasurementData } from "../measurement-data";

describe("useMeasurementData", () => {
  it("aggregates events by second and counts them", () => {
    const md = useMeasurementData();

    md.addMeasurementPoint({
      id: 0,
      event: "callEnd",
      time: 1000,
      msFromStart: 120,
      iteration: 1,
    });
    md.addMeasurementPoint({
      id: 0,
      event: "callEnd",
      time: 2000,
      msFromStart: 80,
      iteration: 2,
    });

    const [counts] = md.getAggregatedData(0, "callEnd");

    expect(counts[1]).toBe(1);
    expect(counts[2]).toBe(1);
  });

  it("computes stage durations from start/end pairs", () => {
    const md = useMeasurementData();

    md.addMeasurementPoint({
      id: 0,
      event: "dnsStart",
      time: 1000,
      msFromStart: 10,
      iteration: 1,
    });
    md.addMeasurementPoint({
      id: 0,
      event: "dnsEnd",
      time: 1000,
      msFromStart: 40,
      iteration: 1,
    });

    const durations = md.getStageDuarations(0, "dns");

    expect(durations).toEqual([30]);
  });

  it("builds table data with response stats and error count", () => {
    const md = useMeasurementData();

    md.addMeasurementPoint({
      id: 0,
      event: "callEnd",
      time: 1000,
      msFromStart: 100,
      iteration: 1,
    });
    md.addMeasurementPoint({
      id: 0,
      event: "callEnd",
      time: 2000,
      msFromStart: 200,
      iteration: 2,
    });
    md.addMeasurementPoint({
      id: 0,
      event: "callFailed",
      time: 3000,
      msFromStart: 50,
      iteration: 3,
    });

    const [row] = md.getTableData(["http://example.com"]);

    expect(row.url).toBe("http://example.com");
    expect(row.count).toBe(2);
    expect(row.countError).toBe(1);
    expect(row.mean).toBe(150);
    expect(row.median).toBe(150);
    expect(row.trimmedMean).toBe(150);
  });
});
