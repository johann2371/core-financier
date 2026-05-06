
package com.corefi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoreFinancierApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreFinancierApplication.class, args);
    }

}
