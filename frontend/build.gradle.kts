import com.github.gradle.node.npm.task.NpmTask

plugins {
  id("com.github.node-gradle.node") version "7.1.0"
}

node {
  version.set("20.10.0")
}

tasks.register<NpmTask>("npmBuild") {
  args.set(listOf("run", "build"))
}
