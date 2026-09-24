package com.futuratecno.application;

import com.futuratecno.api.dto.ComponentePcDTO;
import com.futuratecno.application.ComponentePcExtractor.Tipo;
import com.futuratecno.domain.Producto;
import com.futuratecno.domain.Variante;
import com.futuratecno.infrastructure.ProductoRepository;
import com.futuratecno.infrastructure.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Catálogo de "Armá tu PC". Solo Elit e Invid: son los mayoristas que mandan la ficha técnica
 * completa, y la compatibilidad se lee de ahí. Un listado cargado por JSON lo redacta la IA y no
 * garantiza que el socket o el tipo de RAM estén escritos.
 *
 * <p>Los datos de compatibilidad se calculan en cada request, sin columnas nuevas: son ~800
 * productos y leerlos es regex sobre texto. Si algún día hace falta corregirlos a mano desde el
 * admin, ahí sí conviene guardarlos.
 */
@Service
public class ArmaTuPcService {
    private static final List<String> FUENTES = List.of("ELIT", "INVID");
    private static final int MAX_ESPECIFICACIONES = 220;

    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final CotizacionService cotizacionService;
    private final CategoriaService categoriaService;
    private final PrecioService precioService;

    public ArmaTuPcService(ProductoRepository productoRepository,
                           VarianteRepository varianteRepository,
                           CotizacionService cotizacionService,
                           CategoriaService categoriaService,
                           PrecioService precioService) {
        this.productoRepository = productoRepository;
        this.varianteRepository = varianteRepository;
        this.cotizacionService = cotizacionService;
        this.categoriaService = categoriaService;
        this.precioService = precioService;
    }

    @Transactional(readOnly = true)
    public List<ComponentePcDTO> listarComponentes() {
        List<Producto> candidatos = new ArrayList<>();
        Map<Long, Tipo> tipos = new java.util.HashMap<>();
        for (Producto p : productoRepository.findByActivoAndFuenteIn(true, FUENTES)) {
            var nombres = categoriaService.resolverNombres(p.getCategoriaId());
            Tipo tipo = ComponentePcExtractor.tipo(p.getModelo(),
                    nombres != null ? nombres.getCategoriaPadre() : null,
                    nombres != null ? nombres.getSubcategoria() : null);
            if (tipo == null) continue;
            candidatos.add(p);
            tipos.put(p.getId(), tipo);
        }
        if (candidatos.isEmpty()) return List.of();

        // Mismo criterio que el catálogo: todas las variantes en una consulta, no una por producto.
        Map<Long, List<Variante>> variantesPorProducto = varianteRepository
                .findByProductoIdInAndActivo(candidatos.stream().map(Producto::getId).toList(), true).stream()
                .collect(Collectors.groupingBy(v -> v.getProducto().getId()));

        BigDecimal cotizacion = cotizacionService.obtenerCotizacionUsdArs();
        List<ComponentePcDTO> resultado = new ArrayList<>();
        for (Producto p : candidatos) {
            Tipo tipo = tipos.get(p.getId());
            var nombres = categoriaService.resolverNombres(p.getCategoriaId());
            String hoja = nombres != null ? nombres.getSubcategoria() : null;
            for (Variante v : variantesPorProducto.getOrDefault(p.getId(), List.of())) {
                BigDecimal usd = precioService.precioVentaUsd(v, p, p.getProveedor());
                if (usd == null || usd.signum() <= 0) continue;
                resultado.add(toDTO(p, v, tipo, hoja, usd, precioService.aArs(usd, cotizacion)));
            }
        }
        resultado.sort(Comparator.comparing(ComponentePcDTO::precioUsd));
        return resultado;
    }

    private ComponentePcDTO toDTO(Producto p, Variante v, Tipo tipo, String hoja, BigDecimal usd, BigDecimal ars) {
        String specs = v.getEspecificaciones();
        String modelo = p.getModelo();
        String socket = (tipo == Tipo.PROCESADOR || tipo == Tipo.MOTHER)
                ? ComponentePcExtractor.socket(tipo, modelo, specs) : null;
        String tipoRam = (tipo == Tipo.MOTHER || tipo == Tipo.MEMORIA)
                ? ComponentePcExtractor.tipoRam(tipo, modelo, specs, hoja, socket) : null;
        String formato = (tipo == Tipo.MOTHER || tipo == Tipo.GABINETE)
                ? ComponentePcExtractor.formato(tipo, modelo, specs) : null;
        Integer ranuras = tipo == Tipo.MOTHER ? ComponentePcExtractor.ranurasRam(modelo, specs, formato) : null;
        Integer modulos = tipo == Tipo.MEMORIA ? ComponentePcExtractor.modulosPorUnidad(modelo) : null;
        Integer potencia = tipo == Tipo.FUENTE ? ComponentePcExtractor.potenciaW(modelo, specs) : null;
        Integer recomendada = tipo == Tipo.VIDEO ? ComponentePcExtractor.fuenteRecomendadaW(modelo, specs) : null;
        Boolean video = tipo == Tipo.PROCESADOR ? ComponentePcExtractor.videoIntegrado(modelo, specs) : null;
        Boolean cooler = tipo == Tipo.PROCESADOR ? ComponentePcExtractor.incluyeCooler(modelo, specs) : null;
        Integer gama = tipo == Tipo.PROCESADOR ? ComponentePcExtractor.gamaProcesador(modelo, specs)
                : tipo == Tipo.VIDEO ? ComponentePcExtractor.gamaVideo(modelo, specs) : null;

        String limpia = DescripcionProductoSanitizer.limpiar(specs);
        if (limpia != null && limpia.length() > MAX_ESPECIFICACIONES) {
            limpia = limpia.substring(0, MAX_ESPECIFICACIONES).trim() + "…";
        }
        return new ComponentePcDTO(p.getId(), v.getId(), tipo.name(), p.getMarca(), modelo,
                p.skuCamuflado(), limpia, p.getImagenUrl(), usd, ars,
                socket, tipoRam, formato, ranuras, modulos, potencia, recomendada, video, cooler, gama);
    }
}
