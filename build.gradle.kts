tasks.register("testAll") {
  group = "verification"
  description = "Runs backend tests and frontend Vitest."
  dependsOn(":backend:test")
  dependsOn(":frontend:test")
}
