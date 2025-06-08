

// package crypto;

// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
// import org.springframework.cache.annotation.EnableCaching;
// import org.springframework.scheduling.annotation.EnableScheduling;
// import org.springframework.context.annotation.EnableAspectJAutoProxy;
// import org.springframework.web.reactive.config.EnableWebFlux;

// @SpringBootApplication(scanBasePackages = "crypto.insight.crypto")
// @EnableCaching
// @EnableScheduling
// @EnableWebFlux
// @EnableAspectJAutoProxy
// @ConfigurationPropertiesScan("crypto.insight.crypto.config")
// public class CryptoInsightApplication {
//     public static void main(String[] args) {
//         SpringApplication.run(CryptoInsightApplication.class, args);
//     }
// }

package crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication(scanBasePackages = "crypto.insight.crypto")
@EnableCaching
@EnableScheduling
@EnableWebFlux
@EnableAspectJAutoProxy
@ConfigurationPropertiesScan("crypto.insight.crypto.config")
public class CryptoInsightApplication {
    public static void main(String[] args) {
        // Set system property to disable auto-configuration if needed
        System.setProperty("spring.ai.vertex.ai.gemini.enabled", "true");
        SpringApplication.run(CryptoInsightApplication.class, args);
    }
}