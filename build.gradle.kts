plugins {
    kotlin("jvm") version "1.6.10"
    java
}

group = "de.lookonthebrightsi"
version = "1.0.0-alpha.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.kohsuke:github-api:1.307")
    implementation("net.lingala.zip4j:zip4j:2.11.1")
    implementation("commons-io:commons-io:2.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}