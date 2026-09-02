package com.futuratecno.application;

import com.futuratecno.api.dto.PromocionDTO;
import com.futuratecno.domain.Promocion;
import com.futuratecno.infrastructure.PromocionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromocionService {
    private static final long MAX_BYTES = 500L * 1024L;
    private static final int MAX_ACTIVAS = 10;
    private static final int MIN_PUBLICABLES = 4;
    private final PromocionRepository repository;

    public PromocionService(PromocionRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<PromocionDTO> listarAdmin() {
        return repository.listarResumen().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<PromocionDTO> listarPublicadas() {
        LocalDateTime ahora = java.time.ZonedDateTime.now(PedidoService.ZONA_AR).toLocalDateTime();
        List<PromocionDTO> vigentes = repository.listarResumen().stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> p.getFechaInicio() == null || !p.getFechaInicio().isAfter(ahora))
                .filter(p -> p.getFechaFin() == null || p.getFechaFin().isAfter(ahora))
                .map(this::dto).toList();
        return vigentes.size() >= MIN_PUBLICABLES ? vigentes : List.of();
    }

    @Transactional
    public PromocionDTO crear(String titulo, String texto, String enlace, Integer orden,
                              LocalDateTime inicio, LocalDateTime fin, boolean activo,
                              MultipartFile escritorio, MultipartFile movil) {
        if (escritorio == null || escritorio.isEmpty()) throw new IllegalArgumentException("La imagen de escritorio es obligatoria.");
        validarActivacion(activo, null);
        Promocion p = new Promocion();
        aplicarDatos(p, titulo, texto, enlace, orden, inicio, fin, activo);
        aplicarImagenEscritorio(p, escritorio);
        if (movil != null && !movil.isEmpty()) aplicarImagenMovil(p, movil);
        return dto(repository.save(p));
    }

    @Transactional
    public PromocionDTO actualizar(Long id, String titulo, String texto, String enlace, Integer orden,
                                   LocalDateTime inicio, LocalDateTime fin, boolean activo,
                                   MultipartFile escritorio, MultipartFile movil, boolean quitarMovil) {
        Promocion p = buscar(id);
        validarActivacion(activo, p);
        aplicarDatos(p, titulo, texto, enlace, orden, inicio, fin, activo);
        if (escritorio != null && !escritorio.isEmpty()) aplicarImagenEscritorio(p, escritorio);
        if (quitarMovil) { p.setImagenMovil(null); p.setMimeMovil(null); }
        else if (movil != null && !movil.isEmpty()) aplicarImagenMovil(p, movil);
        return dto(repository.save(p));
    }

    @Transactional public void eliminar(Long id) { repository.delete(buscar(id)); }

    @Transactional(readOnly = true)
    public ImagenBinaria imagen(Long id, boolean movil) {
        Promocion p = buscar(id);
        if (movil && p.getImagenMovil() != null) return new ImagenBinaria(p.getImagenMovil(), p.getMimeMovil());
        return new ImagenBinaria(p.getImagenEscritorio(), p.getMimeEscritorio());
    }

    private void validarActivacion(boolean activo, Promocion actual) {
        if (!activo || (actual != null && Boolean.TRUE.equals(actual.getActivo()))) return;
        if (repository.countByActivoTrue() >= MAX_ACTIVAS) {
            throw new IllegalArgumentException("Solo puede haber 10 promociones activas.");
        }
    }

    private void aplicarDatos(Promocion p, String titulo, String texto, String enlace, Integer orden,
                              LocalDateTime inicio, LocalDateTime fin, boolean activo) {
        if (inicio != null && fin != null && !fin.isAfter(inicio)) throw new IllegalArgumentException("La fecha final debe ser posterior a la inicial.");
        p.setTitulo(limpiar(titulo, 160)); p.setTexto(limpiar(texto, 500));
        p.setEnlace(validarEnlace(enlace)); p.setOrden(orden == null ? 0 : Math.max(0, orden));
        p.setFechaInicio(inicio); p.setFechaFin(fin); p.setActivo(activo);
    }

    private void aplicarImagenEscritorio(Promocion p, MultipartFile file) {
        byte[] bytes = leerYValidar(file, 1600, 600, "La imagen de escritorio"); p.setImagenEscritorio(bytes);
        p.setMimeEscritorio(file.getContentType());
    }
    private void aplicarImagenMovil(Promocion p, MultipartFile file) {
        byte[] bytes = leerYValidar(file, 1080, 1350, "La imagen móvil"); p.setImagenMovil(bytes);
        p.setMimeMovil(file.getContentType());
    }
    private byte[] leerYValidar(MultipartFile file, int ancho, int alto, String nombre) {
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Cada imagen debe pesar como máximo 500 KB.");
        if (!List.of("image/jpeg", "image/webp").contains(file.getContentType())) throw new IllegalArgumentException("Solo se aceptan imágenes JPG o WebP.");
        try {
            byte[] bytes = file.getBytes();
            int[] dimensiones = dimensiones(bytes, file.getContentType());
            if (dimensiones[0] != ancho || dimensiones[1] != alto) {
                throw new IllegalArgumentException(nombre + " debe medir exactamente " + ancho + " × " + alto
                        + " px; mide " + dimensiones[0] + " × " + dimensiones[1] + " px.");
            }
            return bytes;
        } catch (IOException e) { throw new IllegalArgumentException("No se pudo leer la imagen.", e); }
    }
    private String validarEnlace(String value) {
        String v = limpiar(value, 500); if (v == null) return null;
        if (!(v.startsWith("/") || v.startsWith("https://") || v.startsWith("http://"))) throw new IllegalArgumentException("El enlace debe comenzar con /, https:// o http://.");
        return v;
    }
    private String limpiar(String value, int max) { if (value == null || value.isBlank()) return null; String v=value.trim(); return v.length() <= max ? v : v.substring(0,max); }
    private Promocion buscar(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Promoción no encontrada.")); }
    private int[] dimensiones(byte[] bytes, String mime) throws IOException {
        if ("image/jpeg".equals(mime)) {
            var image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) throw new IOException("JPG inválido");
            return new int[]{image.getWidth(), image.getHeight()};
        }
        if (bytes.length < 30 || !new String(bytes, 0, 4).equals("RIFF") || !new String(bytes, 8, 4).equals("WEBP")) throw new IOException("WebP inválido");
        String chunk = new String(bytes, 12, 4);
        if ("VP8X".equals(chunk)) return new int[]{1 + le24(bytes, 24), 1 + le24(bytes, 27)};
        if ("VP8L".equals(chunk) && (bytes[20] & 255) == 0x2f) {
            int b1=bytes[21]&255,b2=bytes[22]&255,b3=bytes[23]&255,b4=bytes[24]&255;
            return new int[]{1 + ((b1 | b2 << 8) & 0x3fff), 1 + (((b2 >> 6) | b3 << 2 | b4 << 10) & 0x3fff)};
        }
        for (int i = 20; i + 6 < bytes.length; i++) {
            if ((bytes[i]&255)==0x9d && (bytes[i+1]&255)==1 && (bytes[i+2]&255)==0x2a)
                return new int[]{((bytes[i+3]&255)|((bytes[i+4]&255)<<8))&0x3fff, ((bytes[i+5]&255)|((bytes[i+6]&255)<<8))&0x3fff};
        }
        throw new IOException("WebP sin dimensiones");
    }
    private int le24(byte[] b, int i) { return (b[i]&255)|((b[i+1]&255)<<8)|((b[i+2]&255)<<16); }
    private PromocionDTO dto(PromocionRepository.PromocionResumen p) {
        String version = p.getUpdatedAt() == null ? "" : "?v=" + p.getUpdatedAt().hashCode();
        String base = "/api/promociones/"+p.getId()+"/imagen/";
        return new PromocionDTO(p.getId(), p.getTitulo(), p.getTexto(), p.getEnlace(), p.getOrden(), p.getFechaInicio(), p.getFechaFin(), Boolean.TRUE.equals(p.getActivo()), base+"escritorio"+version, Boolean.TRUE.equals(p.getTieneImagenMovil()) ? base+"movil"+version : null);
    }
    private PromocionDTO dto(Promocion p) { String v="?v="+(p.getUpdatedAt()!=null?p.getUpdatedAt().hashCode():System.nanoTime()); String base="/api/promociones/"+p.getId()+"/imagen/"; return new PromocionDTO(p.getId(),p.getTitulo(),p.getTexto(),p.getEnlace(),p.getOrden(),p.getFechaInicio(),p.getFechaFin(),Boolean.TRUE.equals(p.getActivo()),base+"escritorio"+v,p.getImagenMovil()!=null?base+"movil"+v:null); }
    public record ImagenBinaria(byte[] bytes, String mime) {}
}
