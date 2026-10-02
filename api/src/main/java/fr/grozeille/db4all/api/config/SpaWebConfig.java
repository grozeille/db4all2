package fr.grozeille.db4all.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the packaged UI statics and falls back to index.html for SPA routes,
 * so deep links (e.g. /login, /projects/{id}/tables/...) work when the UI is
 * served from the jar. Boot's default resource mappings are disabled
 * (spring.web.resources.add-mappings=false) so this is the only mapping.
 * REST controllers and existing files (assets, swagger-ui) take precedence
 * or are served as-is; only missing extensionless non-API paths fall back.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/META-INF/resources/",
                        "classpath:/resources/",
                        "classpath:/static/",
                        "classpath:/public/")
                .resourceChain(false)
                .addResolver(new SpaFallbackResourceResolver());
    }

    static class SpaFallbackResourceResolver extends PathResourceResolver {

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = location.createRelative(resourcePath);
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            if (isSpaRoute(resourcePath)) {
                Resource index = location.createRelative("index.html");
                if (index.exists() && index.isReadable()) {
                    return index;
                }
            }
            return null;
        }

        private boolean isSpaRoute(String resourcePath) {
            if (resourcePath.startsWith("api/")
                    || resourcePath.startsWith("v3/")
                    || resourcePath.startsWith("swagger-ui")) {
                return false;
            }
            String lastSegment = resourcePath.contains("/")
                    ? resourcePath.substring(resourcePath.lastIndexOf('/') + 1)
                    : resourcePath;
            return !lastSegment.contains(".");
        }
    }
}
