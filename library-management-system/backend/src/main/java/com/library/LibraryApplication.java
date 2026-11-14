package com.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Shelfwise library management system.
 *
 * Code is organised by feature: catalog (books and copies), circulation (loans),
 * hold, fine, member, notification, audit, report, settings and dashboard.
 * Each feature owns its entities, repositories, service and controller.
 */
@SpringBootApplication
@EnableScheduling
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
