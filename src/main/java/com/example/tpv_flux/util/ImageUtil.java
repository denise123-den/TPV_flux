package com.example.tpv_flux.util;

import com.example.tpv_flux.HelloApplication;
import com.example.tpv_flux.model.ProductItem;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
 * 2. Mapeo directo a las imágenes en '/imagenes/' según el nombre o etiqueta del producto.
 * 3. Búsqueda automática por SKU o etiqueta corta en '/imagenes/' y '/com/example/tpv_flux/images/productos/'.
 */
public final class ImageUtil {

    public static final String RESOURCES_IMAGE_DIR = "/com/example/tpv_flux/images/productos/";
    public static final String ALT_RESOURCES_IMAGE_DIR = "/imagenes/";
    private static final List<String> EXTENSIONS = List.of(".jpeg", ".jpg", ".png", ".webp", ".gif");
    private static final List<String> LEGACY_PLACEHOLDERS = List.of(
            "d1.jpeg", "biblio.jpeg", "upp.jpeg", "logo.png", "logotarea.png"
    );
    private static final Map<String, Image> IMAGE_CACHE = new ConcurrentHashMap<>();

    private ImageUtil() {
    }

    public static void clearCache() {
        IMAGE_CACHE.clear();
    }

    public static boolean isLegacyPlaceholder(String photoRef) {
        if (photoRef == null || photoRef.isBlank()) {
            return false;
        }
        String clean = photoRef.trim();
        for (String legacy : LEGACY_PLACEHOLDERS) {
            if (legacy.equalsIgnoreCase(clean) || clean.toLowerCase(Locale.ROOT).endsWith("/" + legacy) || clean.toLowerCase(Locale.ROOT).endsWith("\\" + legacy)) {
                return true;
            }
        }
        return false;
    }

    public static String resolvePhotoReference(String currentPhoto, String productName, String shortTag) {
        if (currentPhoto != null && !currentPhoto.isBlank() && !isLegacyPlaceholder(currentPhoto)) {
            String trimmed = currentPhoto.trim();
            if (loadFromReference(trimmed) != null) {
                return trimmed;
            }
        }
        String mapped = resolveDefaultFileName(productName, shortTag);
        return mapped != null ? mapped : currentPhoto;
    }

    public static Image resolveProductImage(ProductItem product) {
        if (product == null) {
            return null;
        }

        // 1. Intentar con la ruta/referencia explícita guardada en la columna 'foto' de la BD (ignorando placeholders antiguos eliminados)
        String photoRef = product.getPhoto();
        if (photoRef != null && !photoRef.isBlank() && !isLegacyPlaceholder(photoRef)) {
            Image fromRef = loadFromReference(photoRef.trim());
            if (fromRef != null) {
                return fromRef;
            }
        }

        // 2. Resolver por nombre o etiqueta del producto hacia las imágenes de src/main/resources/imagenes/
        String mappedFileName = resolveDefaultFileName(product.getName(), product.getShortTag());
        if (mappedFileName != null) {
            Image fromMapped = loadFromReference(mappedFileName);
            if (fromMapped != null) {
                return fromMapped;
            }
        }

        // 3. Buscar automáticamente por SKU (ej. FN-IP14-001.jpeg)
        if (product.getSku() != null && !product.getSku().isBlank()) {
            Image bySku = findInResourcesByBaseName(product.getSku().trim());
            if (bySku != null) {
                return bySku;
            }
        }

        // 4. Buscar automáticamente por etiqueta corta
        if (product.getShortTag() != null && !product.getShortTag().isBlank()) {
            Image byTag = findInResourcesByBaseName(product.getShortTag().trim());
            if (byTag != null) {
                return byTag;
            }
        }

        return null;
    }

    /**
     * Devuelve el nombre de archivo de imagen en src/main/resources/imagenes/ correspondiente a cada producto.
     */
    public static String resolveDefaultFileName(String productName, String shortTag) {
        String nameLower = productName != null ? productName.toLowerCase(Locale.ROOT) : "";
        String tagUpper = shortTag != null ? shortTag.trim().toUpperCase(Locale.ROOT) : "";

        if (nameLower.contains("funda") || "FUNDA".equals(tagUpper)) {
            return "iphon.jpeg";
        }
        if (nameLower.contains("cable") || "CABLE".equals(tagUpper)) {
            return "USB.jpeg";
        }
        if (nameLower.contains("aud") || "AUDIO".equals(tagUpper)) {
            return "audiculares.jpeg";
        }
        if (nameLower.contains("cargador") || "CARGA".equals(tagUpper)) {
            return "Cargador.jpeg";
        }
        if (nameLower.contains("mica") || "MICA".equals(tagUpper)) {
            return "Mica.jpeg";
        }
        if (nameLower.contains("power") || "POWER".equals(tagUpper)) {
            return "Power.jpeg";
        }
        if (nameLower.contains("memoria") || "USB".equals(tagUpper)) {
            return "Memoria.jpeg";
        }
        if (nameLower.contains("soporte") || "SOPORTE".equals(tagUpper)) {
            return "Soporte.jpeg";
        }
        return null;
    }

    public static Image loadFromReference(String ref) {
        if (ref == null || ref.isBlank()) {
            return null;
        }
        String trimmed = ref.trim();
        Image cached = IMAGE_CACHE.get(trimmed);
        if (cached != null) {
            return cached;
        }
        Image loaded = loadInternal(trimmed);
        if (loaded != null) {
            IMAGE_CACHE.put(trimmed, loaded);
        }
        return loaded;
    }

    private static Image loadInternal(String ref) {
        try {
            // Caso A: URL web o file://
            if (ref.startsWith("http://") || ref.startsWith("https://") || ref.startsWith("file:")) {
                Image img = new Image(ref, true);
                if (!img.isError()) {
                    return img;
                }
                return loadFromUrl(new URL(ref));
            }

            // Caso B: Archivo directo en el disco duro (ej. C:/imagenes/funda.png o ruta relativa)
            File diskFile = new File(ref);
            if (diskFile.exists() && diskFile.isFile()) {
                Image img = loadFromPath(diskFile.toPath());
                if (img != null) {
                    return img;
                }
            }

            // Caso C: Archivo dentro de src/main/resources/imagenes/ o src/main/resources/com/example/tpv_flux/images/productos/
            Path altDevPath = Paths.get("src", "main", "resources", "imagenes", ref);
            if (Files.exists(altDevPath) && Files.isRegularFile(altDevPath)) {
                Image img = loadFromPath(altDevPath);
                if (img != null) {
                    return img;
                }
            }

            Path devResourcePath = Paths.get("src", "main", "resources", "com", "example", "tpv_flux", "images", "productos", ref);
            if (Files.exists(devResourcePath) && Files.isRegularFile(devResourcePath)) {
                Image img = loadFromPath(devResourcePath);
                if (img != null) {
                    return img;
                }
            }

            // Caso D: Recurso en el classpath (busca en /imagenes/ y en /com/example/tpv_flux/images/productos/)
            String[] candidateLocations = ref.startsWith("/")
                    ? new String[]{ref}
                    : new String[]{ALT_RESOURCES_IMAGE_DIR + ref, RESOURCES_IMAGE_DIR + ref};

            for (String classpathLocation : candidateLocations) {
                URL url = HelloApplication.class.getResource(classpathLocation);
                if (url != null) {
                    Image img = loadFromUrl(url);
                    if (img != null) {
                        return img;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Image loadFromPath(Path path) {
        Image img = new Image(path.toUri().toString(), false);
        if (!img.isError()) {
            return img;
        }
        try (InputStream is = Files.newInputStream(path)) {
            return decodeWithImageIO(is);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Image loadFromUrl(URL url) {
        try (InputStream is = url.openStream()) {
            Image img = new Image(is);
            if (!img.isError()) {
                return img;
            }
        } catch (Exception ignored) {
        }
        try (InputStream is = url.openStream()) {
            return decodeWithImageIO(is);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Image decodeWithImageIO(InputStream is) {
        try {
            BufferedImage buffered = ImageIO.read(is);
            if (buffered == null) {
                return null;
            }
            int w = buffered.getWidth();
            int h = buffered.getHeight();
            WritableImage writable = new WritableImage(w, h);
            PixelWriter pw = writable.getPixelWriter();
            int[] argb = buffered.getRGB(0, 0, w, h, null, 0, w);
            pw.setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), argb, 0, w);
            return writable;
        } catch (Exception ignored) {
            return null;
        }
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
