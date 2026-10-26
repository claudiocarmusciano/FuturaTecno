package com.futuratecno.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futuratecno.api.dto.ArticuloJsonDTO;
import com.futuratecno.infrastructure.ProveedorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * El panel "Texto del proveedor" con Cozzo elegido: aplica {@link ReglasCozzo} al texto antes de la
 * IA y al borrador después. En el borrador saca lo que la carga omitiría igual y deja UNA fila por
 * artículo, la más cara: si no, el panel marca las repetidas como duplicado y no deja importar.
 */
@Service
public class ListadoCozzoService {

    private final ProveedorRepository proveedores;
    private final CargaJsonService carga;
    private final ObjectMapper mapper;

    public ListadoCozzoService(ProveedorRepository proveedores, CargaJsonService carga, ObjectMapper mapper) {
        this.proveedores = proveedores; this.carga = carga; this.mapper = mapper;
    }

    public boolean esCozzo(Long proveedorId) {
        return proveedorId != null && proveedores.findById(proveedorId).map(ReglasCozzo::aplica).orElse(false);
    }

    public GenerarListadoService.Borrador depurar(GenerarListadoService.Borrador borrador) {
        List<Map<String, Object>> filas = borrador.articulos();
        List<ArticuloJsonDTO> dtos = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            ArticuloJsonDTO dto;
            try { dto = mapper.convertValue(fila, ArticuloJsonDTO.class); } catch (IllegalArgumentException e) { dto = new ArticuloJsonDTO(); }
            dtos.add(dto);
        }
        List<Map<String, Object>> identidades = carga.identidades(dtos);

        int fuera = 0;
        Map<String, Integer> masCaro = new LinkedHashMap<>();
        List<Integer> sinClave = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            ArticuloJsonDTO a = dtos.get(i);
            if (ReglasCozzo.motivo(a.getModelo(), a.getModeloExacto(), a.getPrecioUsd()) != null) { fuera++; continue; }
            Object clave = identidades.get(i).get("identidad");
            if (clave == null || a.getPrecioUsd() == null) { sinClave.add(i); continue; }
            Integer previo = masCaro.get(clave.toString());
            if (previo == null) masCaro.put(clave.toString(), i);
            else {
                fuera++;
                BigDecimal actual = dtos.get(previo).getPrecioUsd();
                if (a.getPrecioUsd().compareTo(actual) > 0) masCaro.put(clave.toString(), i);
            }
        }
        TreeSet<Integer> quedan = new TreeSet<>(masCaro.values());
        quedan.addAll(sinClave);
        List<Map<String, Object>> salida = quedan.stream().map(filas::get).toList();

        List<String> avisos = new ArrayList<>(borrador.avisos() == null ? List.of() : borrador.avisos());
        if (fuera > 0) avisos.add("Cozzo: se sacaron " + fuera + " filas (repetidas con otro precio, por debajo de US$ "
                + ReglasCozzo.PRECIO_MINIMO_USD + ", ACR o versión CH). De las repetidas quedó la más cara.");
        return new GenerarListadoService.Borrador(salida, avisos);
    }
}
