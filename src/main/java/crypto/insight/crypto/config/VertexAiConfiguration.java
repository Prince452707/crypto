package crypto.insight.crypto.config;

import com.google.cloud.vertexai.VertexAI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VertexAiConfiguration {

    @ConfigurationProperties(prefix = "spring.ai.vertex.ai.gemini")
    public static class VertexAiProperties {
        private String projectId;
        private String location;
        private String model;

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.vertex.ai.gemini.project-id")
    public VertexAI vertexAI(VertexAiProperties properties) {
        return new VertexAI.Builder()
                .setProjectId(properties.getProjectId())
                .setLocation(properties.getLocation())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.vertex.ai.gemini.project-id")
    public VertexAiProperties vertexAiProperties() {
        return new VertexAiProperties();
    }
}