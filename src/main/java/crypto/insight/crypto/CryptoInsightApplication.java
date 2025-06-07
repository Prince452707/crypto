package crypto.insight.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication(scanBasePackages = "crypto.insight.crypto")
@EnableCaching
@EnableScheduling
@EnableWebFlux
@EnableAspectJAutoProxy
public class CryptoInsightApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoInsightApplication.class, args);
    }
}
