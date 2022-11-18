package com.paperless.service

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableAutoConfiguration(exclude = [org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration::class])
class PaperlessServiceApplication

fun main(args: Array<String>) {
	runApplication<PaperlessServiceApplication>(*args)
}
