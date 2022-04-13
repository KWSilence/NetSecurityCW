import org.jetbrains.kotlin.konan.properties.loadProperties

val ktorVersion = "2.0.0"
val kotlinVersion = "1.6.10"
val logbackVersion = "1.2.11"
val exposedVersion = "0.38.1"
val postgresVersion = "42.3.3"
val jwtVersion = "0.11.2"

val debug: String by project
val baseUrl: String by project

val secretProperties = loadProperties("${projectDir.path}/secrets/secret.properties")

val emailName = secretProperties["email_name"] as String
val emailPass = secretProperties["email_pass"] as String
val sharePath = secretProperties["share_path"] as String

val dbUrl = secretProperties["db_url"] as String
val dbDriver = secretProperties["db_driver"] as String
val dbUser = secretProperties["db_user"] as String
val dbPass = secretProperties["db_pass"] as String

val jwtSecret = secretProperties["jwt_secret"] as String

plugins {
    application
    kotlin("jvm") version "1.6.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.github.gmazzo.buildconfig") version "3.0.3"
}

group = "com.kwsilence"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

//    val isDevelopment: Boolean = project.ext.has("development")
//    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

buildConfig {
    buildConfigField("Boolean", "debug", debug)
    buildConfigField("String", "baseUrl", "\"$baseUrl\"")

    buildConfigField("String", "emailName", "\"$emailName\"")
    buildConfigField("String", "emailPass", "\"$emailPass\"")
    buildConfigField("String", "sharePath", "\"$sharePath\"")

    buildConfigField("String", "dbUrl", "\"$dbUrl\"")
    buildConfigField("String", "dbDriver", "\"$dbDriver\"")
    buildConfigField("String", "dbUser", "\"$dbUser\"")
    buildConfigField("String", "dbPass", "\"$dbPass\"")

    buildConfigField("String", "jwtSecret", "\"$jwtSecret\"")
}

tasks {
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "com.kwsilence.Application"))
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-compression-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-locations:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")
    implementation("io.ktor:ktor-server-http-redirect-jvm:$ktorVersion")

    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

    implementation(platform("org.jetbrains.exposed:exposed-bom:$exposedVersion"))
    implementation("org.jetbrains.exposed", "exposed-core")
    implementation("org.jetbrains.exposed", "exposed-dao")
    implementation("org.jetbrains.exposed", "exposed-jdbc")
    implementation("org.postgresql:postgresql:$postgresVersion")

    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jwtVersion")

    implementation("javax.mail:mail:1.5.0-b01")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}