import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
  java
  id("org.springframework.boot") version "4.0.1"
  id("io.spring.dependency-management") version "1.1.7"
}

group = "com.github.anvaer.webpecker"
version = "0.0.1-SNAPSHOT"
description = ""

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-jetty")
  implementation("org.springframework.boot:spring-boot-starter-websocket") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
  }
  implementation("com.squareup.okhttp3:okhttp:5.1.0")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0") {
    exclude(group = "net.bytebuddy", module = "byte-buddy")
  }
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<Copy>("copyWebApp") {
  from("$rootDir/frontend/dist")
  into(layout.buildDirectory.dir("resources/main/static"))
}

tasks.named<JavaCompile>("compileJava") {
  dependsOn(":frontend:npmBuild")
}

tasks.named<ProcessResources>("processResources") {
  dependsOn("copyWebApp")
}

tasks.named<Test>("test") {
  useJUnitPlatform()
}
