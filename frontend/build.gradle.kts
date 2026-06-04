import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("com.github.node-gradle.node") version "7.1.0"
}

node {
  version.set("20.10.0")
}

val nodeModulesDir = layout.projectDirectory.dir("node_modules")

tasks.register<NpmTask>("npmBuild") {
  dependsOn("npmInstall")
  args.set(listOf("run", "build"))
  inputs.file(layout.projectDirectory.file("package.json"))
  inputs.file(layout.projectDirectory.file("vue.config.js"))
  inputs.dir(layout.projectDirectory.dir("src"))
  inputs.dir(layout.projectDirectory.dir("public"))
  outputs.dir(layout.projectDirectory.dir("dist"))
}

tasks.register<NpmTask>("test") {
  dependsOn("npmInstall")
  args.set(listOf("run", "test:run"))
}

tasks.named("npmInstall") {
  onlyIf {
    !nodeModulesDir.asFile.exists()
  }
  notCompatibleWithConfigurationCache("npmInstall uses external node environment")
}
