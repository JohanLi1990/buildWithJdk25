import org.gradle.kotlin.dsl.implementation

plugins {
    id("java")
}

group = "org.chenyang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/com.lmax/disruptor
    implementation("com.lmax:disruptor:4.0.0")
    implementation("io.netty:netty-all:4.2.7.Final")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.17")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    // https://mvnrepository.com/artifact/org.openjdk.jol/jol-core
    implementation("org.openjdk.jol:jol-core:0.17")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("ch.qos.logback:logback-core:1.5.21")
    // Source: https://mvnrepository.com/artifact/org.hdrhistogram/HdrHistogram
    implementation("org.hdrhistogram:HdrHistogram:2.2.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}