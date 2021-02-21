/*
 * This file was generated by the Gradle 'init' task.
 */
import com.google.protobuf.gradle.*

plugins {
    java
    `maven-publish`
    id("com.google.protobuf") version "0.8.8"
    signing
    id("io.codearte.nexus-staging") version "0.22.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("org.zeromq:jeromq:0.5.2")
    implementation("net.bytebuddy:byte-buddy:1.10.20")
    implementation("org.apache.commons:commons-lang3:3.11")
    implementation("org.apache.zookeeper:zookeeper:3.6.2")
    implementation("com.esotericsoftware:kryo:5.0.3")
    implementation("org.apache.clerezza.ext:org.json.simple:0.4")
    implementation("org.apache.kafka:kafka_2.13:2.7.0")
    implementation("org.springframework.amqp:spring-rabbit:2.3.5")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")
    implementation("com.google.guava:guava:30.1-jre")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.0")
    implementation("io.grpc:grpc-netty:1.34.1")
    implementation("io.grpc:grpc-protobuf:1.34.1")
    implementation("io.grpc:grpc-stub:1.34.1")
    implementation("io.netty:netty-tcnative:2.0.35.Final")
    implementation("org.jetbrains:annotations:20.1.0")
    testImplementation("org.springframework:spring-test:5.3.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.apache.curator:curator-test:5.1.0")
    testImplementation("org.springframework.kafka:spring-kafka-test:2.6.6")
    testImplementation("commons-io:commons-io:2.8.0")
    testImplementation("com.github.fridujo:rabbitmq-mock:1.1.1")
    compileOnly("org.springframework:spring-beans:5.3.4")
    compileOnly("org.springframework:spring-context:5.3.4")
    compileOnly("org.springframework:spring-aspects:5.3.4")
    compileOnly("org.projectlombok:lombok:1.18.18")
    testCompileOnly("org.projectlombok:lombok:1.18.18")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.18")
    annotationProcessor("org.projectlombok:lombok:1.18.18")
    protobuf(files("proto/"))
}

group = "com.github.dredwardhyde"
version = "1.11"
description = "Jaffa RPC Library"
java.sourceCompatibility = JavaVersion.VERSION_1_8

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.15.1"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

sourceSets.getByName("main") {
    java.srcDir("build/generated/source/proto/main/java")
    java.srcDir("build/generated/source/proto/main/grpc")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name.set("Jaffa RPC Library")
                description.set("A high performance RPC library for Java 8+ and Spring Framework")
                url.set("https://github.com/dredwardhyde/jaffa-rpc-library")
                inceptionYear.set("2019")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("dredwardhyde")
                        name.set("Nikita L")
                        email.set("komesergey@gmail.com")
                        roles.addAll(listOf("owner", "developer"))
                    }
                }
                scm {
                    connection.set("scm:https://github.com/dredwardhyde/jaffa-rpc-library.git")
                    developerConnection.set("scm:git://github.com/dredwardhyde/jaffa-rpc-library.git")
                    url.set("https://github.com/dredwardhyde/jaffa-rpc-library")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}