package com.futuratecno.application;

import com.futuratecno.domain.Producto;
import com.futuratecno.infrastructure.ProductoRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductoRepository de mentira, sin Mockito. Mockear esta interfaz fallaba al correr la suite
 * completa (AssertionError interno de Mockito en MockUtil con la JVM nueva y Byte Buddy
 * experimental), aunque cada test pasaba solo. Responde lo justo para las importaciones de mayoristas.
 */
final class ProductoRepositoryFalso {

    final Map<String, List<Producto>> porCodigo = new ConcurrentHashMap<>();
    final List<Producto> guardados = new ArrayList<>();
    boolean bloqueoLibre = true;

    void agregar(Long proveedorId, String codigo, Producto p) {
        porCodigo.computeIfAbsent(proveedorId + "|" + codigo, k -> new ArrayList<>()).add(p);
    }

    ProductoRepository repo() {
        InvocationHandler h = (proxy, metodo, args) -> {
            if (metodo.isDefault()) return InvocationHandler.invokeDefault(proxy, metodo, args);
            return switch (metodo.getName()) {
                case "findAllByProveedorIdAndCodigoExternoOrderByActivoDescIdAsc" ->
                        porCodigo.getOrDefault(args[0] + "|" + args[1], List.of());
                case "intentarBloqueoImportacion" -> bloqueoLibre;
                case "save" -> { guardados.add((Producto) args[0]); yield args[0]; }
                case "saveAll" -> args[0];
                case "toString" -> "ProductoRepositoryFalso";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> {
                    Class<?> r = metodo.getReturnType();
                    if (r == Optional.class) yield Optional.empty();
                    if (List.class.isAssignableFrom(r)) yield List.of();
                    if (r == boolean.class) yield false;
                    if (r == int.class || r == long.class) yield 0;
                    yield null;
                }
            };
        };
        return (ProductoRepository) Proxy.newProxyInstance(ProductoRepository.class.getClassLoader(),
                new Class<?>[]{ProductoRepository.class}, h);
    }
}
