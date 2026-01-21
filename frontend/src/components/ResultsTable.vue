<template>
  <DataTable
    :value="tableData"
    :data-key="id"
    :scrollable="true"
    scroll-height="100%"
    class="flex-1"
  >
    <Column field="id" header="#" sortable></Column>
    <Column field="url" header="URL" sortable></Column>
    <Column field="count" header="Successful" sortable></Column>
    <Column field="countError" header="Failed" sortable></Column>
    <Column field="mean" header="Mean" sortable></Column>
    <Column field="median" header="Median" sortable></Column>
    <Column field="trimmedMean" header="Trimmed Mean" sortable></Column
  ></DataTable>
</template>
<script>
import { DataTable, Column } from "primevue";

export default {
  name: "UrlLoaderDialog",
  components: {
    DataTable,
    Column,
  },
  props: {
    tableData: {
      type: Array,
      default() {
        return [];
      },
    },
  },
  setup() {},
};
</script>
