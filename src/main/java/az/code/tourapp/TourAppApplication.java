package az.code.tourapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class TourAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TourAppApplication.class, args);
    }
}
