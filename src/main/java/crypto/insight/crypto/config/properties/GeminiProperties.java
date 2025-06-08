package crypto.insight.crypto.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiProperties {
    
    private final String projectId;
    private final String location;
    private final String modelName;
    
    @ConstructorBinding
    public GeminiProperties(String projectId, String location, String modelName) {
        this.projectId = projectId;
        this.location = location;
        this.modelName = modelName;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public String getLocation() {
        return location;
    }
    
    public String getModelName() {
        return modelName;
    }
    
    @Override
    public String toString() {
        return "GeminiProperties{" +
                "projectId='" + projectId + '\'' +
                ", location='" + location + '\'' +
                ", modelName='" + modelName + '\'' +
                '}';
    }
}