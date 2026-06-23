package com.futuratecno.infrastructure;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sirve el frontend (React/Vite) embebido en classpath:/static/ y hace fallback a
 * index.html para las rutas del cliente (ej: /producto/5, /admin), respetando el
 * ruteo de React Router. Las rutas /api/** quedan para los controllers.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource recurso = location.createRelative(resourcePath);
                        if (recurso.exists() && recurso.isReadable()) {
                            return recurso;
                        }
                        // No es un archivo estático: si es API, no lo resolvemos acá (lo toman los controllers).
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        // Cualquier otra ruta del cliente → index.html (SPA).
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
