package com.example.tpv_flux.util;

import com.example.tpv_flux.HelloApplication;
import com.example.tpv_flux.model.ProductItem;
import javafx.scene.image.Image;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilidad para cargar imágenes de productos desde:
 * 1. La referencia guardada en la columna 'foto' de PostgreSQL (nombre de archivo, ruta de recurso, ruta en disco o URL).
 * 2. Carpeta de recursos '/com/example/tpv_flux/images/productos/' buscando automáticamente por SKU o etiqueta corta.
 */
public final class ImageUtil {

    public static final String RESOURCES_IMAGE_DIR = "/com/example/tpv_flux/images/productos/";
    public static final String ALT_RESOURCES_IMAGE_DIR = "/imagenes/";
    private static final List<String> EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".webp", ".gif");
    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();

    private ImageUtil() {
    }

    public static void clearCache() {
        IMAGE_CACHE.clear();
    }

    public static Image resolveProductImage(ProductItem product) {
        if (product == null) {
            return null;
        }

        // 1. Intentar con la ruta/referencia explícita guardada en la columna 'foto' de la BD
        String photoRef = product.getPhoto();
        if (photoRef != null && !photoRef.isBlank()) {
            Image fromRef = loadFromReference(photoRef.trim());
            if (fromRef != null) {
                return fromRef;
            }
        }

        // 2. Buscar automáticamente por SKU (ej. FN-IP14-001.png) en resources/com/example/tpv_flux/images/productos/
        if (product.getSku() != null && !product.getSku().isBlank()) {
            Image bySku = findInResourcesByBaseName(product.getSku().trim());
            if (bySku != null) {
                return bySku;
            }
        }

        // 3. Buscar automáticamente por etiqueta corta en minúsculas (ej. funda.png, cable.png, audio.png)
        if (product.getShortTag() != null && !product.getShortTag().isBlank()) {
            Image byTag = findInResourcesByBaseName(product.getShortTag().trim().toLowerCase(Locale.ROOT));
            if (byTag != null) {
                return byTag;
            }
        }

        return null;
    }

    public static Image loadFromReference(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        return IMAGE_CACHE.computeIfAbsent(ref, ImageUtil::loadInternal);
    }

    private static Image loadInternal(String ref) {
        try {
            // Caso A: URL web o file://
            if (ref.startsWith("http://") || ref.startsWith("https://") || ref.startsWith("file:")) {
                Image img = new Image(ref, true);
                return img.isError() ? null : img;
            }

            // Caso B: Archivo directo en el disco duro (ej. C:/imagenes/funda.png o ruta relativa)
            File diskFile = new File(ref);
            if (diskFile.exists() && diskFile.isFile()) {
                Image img = new Image(diskFile.toURI().toString(), false);
                return img.isError() ? null : img;
            }

            // Caso C: Archivo dentro de src/main/resources/imagenes/ o src/main/resources/com/example/tpv_flux/images/productos/
            Path altDevPath = Paths.get("src", "main", "resources", "imagenes", ref);
            if (Files.exists(altDevPath) && Files.isRegularFile(altDevPath)) {
                Image img = new Image(altDevPath.toUri().toString(), false);
                return img.isError() ? null : img;
            }

            Path devResourcePath = Paths.get("src", "main", "resources", "com", "example", "tpv_flux", "images", "productos", ref);
            if (Files.exists(devResourcePath) && Files.isRegularFile(devResourcePath)) {
                Image img = new Image(devResourcePath.toUri().toString(), false);
                return img.isError() ? null : img;
            }

            // Caso D: Recurso en el classpath (busca en /imagenes/ y en /com/example/tpv_flux/images/productos/)
            String[] candidateLocations = ref.startsWith("/")
                    ? new String[]{ref}
                    : new String[]{ALT_RESOURCES_IMAGE_DIR + ref, RESOURCES_IMAGE_DIR + ref};

            for (String classpathLocation : candidateLocations) {
                URL url = HelloApplication.class.getResource(classpathLocation);
                if (url != null) {
                    try (InputStream is = url.openStream()) {
                        Image img = new Image(is);
                        if (!img.isError()) {
                            return img;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Image findInResourcesByBaseName(String baseName) {
        for (String ext : EXTENSIONS) {
            String candidate = baseName + ext;
            Image img = loadFromReference(candidate);
            if (img != null) {
                return img;
            }
            String lowerCandidate = baseName.toLowerCase(Locale.ROOT) + ext;
            if (!lowerCandidate.equals(candidate)) {
                img = loadFromReference(lowerCandidate);
                if (img != null) {
                    return img;
                }
            }
        }
        return null;
    }
}
