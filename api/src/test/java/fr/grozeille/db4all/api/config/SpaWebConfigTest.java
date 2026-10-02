package fr.grozeille.db4all.api.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SpaWebConfigTest {

    private final SpaWebConfig.SpaFallbackResourceResolver resolver =
            new SpaWebConfig.SpaFallbackResourceResolver();

    @Test
    void fallsBackToIndexForSpaRoutes(@TempDir Path dir) throws IOException {
        Resource location = staticDir(dir);

        assertThat(resolver.getResource("login", location).getFile().getName())
                .isEqualTo("index.html");
        assertThat(resolver.getResource("projects/123/tables/new", location).getFile().getName())
                .isEqualTo("index.html");
    }

    @Test
    void servesExistingFiles(@TempDir Path dir) throws IOException {
        Resource location = staticDir(dir);
        Files.createDirectories(dir.resolve("assets"));
        Files.writeString(dir.resolve("assets/app.js"), "console.log(1)");

        assertThat(resolver.getResource("assets/app.js", location).getFile().getName())
                .isEqualTo("app.js");
    }

    @Test
    void returnsNullForApiSwaggerAndMissingAssets(@TempDir Path dir) throws IOException {
        Resource location = staticDir(dir);

        assertThat(resolver.getResource("api/v2/unknown", location)).isNull();
        assertThat(resolver.getResource("v3/api-docs", location)).isNull();
        assertThat(resolver.getResource("swagger-ui-bundle.js", location)).isNull();
        assertThat(resolver.getResource("assets/missing.js", location)).isNull();
    }

    private Resource staticDir(Path dir) throws IOException {
        Files.writeString(dir.resolve("index.html"), "<html></html>");
        return new FileSystemResource(dir + "/");
    }
}
