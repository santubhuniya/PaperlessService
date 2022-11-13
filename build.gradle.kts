import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.7.4"
	id("io.spring.dependency-management") version "1.0.14.RELEASE"
	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
}

group = "com.paperless"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_14

repositories {
	mavenCentral()
}

dependencies {
	//implementation("org.springframework.boot:spring-boot-starter-web")
	//implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	//    JWT
//	implementation("io.jsonwebtoken:jjwt-api:0.11.1")
//	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.1")
//	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.1")
	//coroutine
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	//gson
	implementation("com.google.code.gson:gson:2.3.1")


	//dtabase
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("dev.miku:r2dbc-mysql:0.8.2.RELEASE")
	implementation("io.r2dbc:r2dbc-pool")
	runtimeOnly("mysql:mysql-connector-java")

	//flow api rsocket
	implementation("org.springframework.boot:spring-boot-starter-rsocket")


//	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "14"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
