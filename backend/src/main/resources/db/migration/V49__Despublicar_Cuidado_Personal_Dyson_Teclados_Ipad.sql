-- 2026-10-28, pedido del usuario: sacar de la tienda lo que ExclusionesCarga ya no deja cargar:
-- cuidado personal, toda la marca Dyson y los teclados de iPad. Se desactivan (no se borran),
-- así los pedidos viejos siguen apuntando a su producto y se puede revertir.
UPDATE productos p SET activo = false
FROM (SELECT id,
             CASE WHEN marca IS NOT NULL AND lower(modelo) LIKE lower(marca) || ' %'
                  THEN substr(modelo, length(marca) + 2) ELSE modelo END AS sin_marca
      FROM productos) s
WHERE p.id = s.id AND p.activo AND (
      p.marca ~* '^\s*dyson\s*$' OR p.modelo ~* '\mdyson\M'
   OR (s.sin_marca ~* '^\s*(apple\s+)?((magic|smart)\s+)?(keyboard|teclado)\M' AND p.modelo ~* '\mipad')
   OR s.sin_marca ~* '^\s*((smart|mini|digital|el[eé]ctric[ao]s?|profesional(es)?)\s+)*(secador(es)?\M(?!\s+de\s+ropa)|secadoras?\s+de\s+(pelo|cabello)|planchitas?|planchas?\s+(de|para)\s+(pelo|cabello)|alisador|rizador|bucleras?|ondulador|afeitadoras?|m[aá]quinas?\s+(de|para)\s+(afeitar|cortar)|cortadoras?\s+(de|para)\s+(pelo|cabello|barba)|cortapelos?|recortadoras?|trimmers?|depiladoras?|cepillos?\s+(secador|alisador|el[eé]ctrico|dental|de\s+dientes)|hair\s*(dryer|straightener|clipper)|shavers?|groomers?|epilators?)'
);
