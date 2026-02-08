package juliokozarewicz.accounts.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${PUBLIC_DOMAIN}")
    private String publicDomain;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        String[] allowedOrigins = publicDomain.split(",");
        List<String> processedOrigins = new ArrayList<>();

        for (String origin : allowedOrigins) {
            origin = origin.trim();

            if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
                processedOrigins.add("http://" + origin);
                processedOrigins.add("https://" + origin);
            } else {
                processedOrigins.add(origin);
            }
        }

        registry.addMapping("/**")
            .allowedOrigins(processedOrigins.toArray(new String[0]))
            .allowedMethods("*")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}