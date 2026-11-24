import { formatPesos } from './minimoCompra'

const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
const usd = n => Number(n || 0).toLocaleString('es-AR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })

/**
 * Abre el presupuesto de "Armá tu PC" en una ventana aparte, lista para imprimir o guardar como PDF.
 * `items`: [{ titulo, marca, modelo, cantidad, precioArs, precioUsd }] con precios UNITARIOS.
 */
export function imprimirPresupuesto({ nombre, items, totalArs, totalUsd, fecha = new Date(), cliente }) {
  const filas = items.map(i => `<tr>
      <td>${esc(i.titulo)}</td>
      <td>${esc([i.marca, i.modelo].filter(Boolean).join(' '))}</td>
      <td class="n">${i.cantidad}</td>
      <td class="n">$ ${formatPesos(Number(i.precioArs) * i.cantidad)}</td>
      <td class="n">US$ ${usd(Number(i.precioUsd) * i.cantidad)}</td>
    </tr>`).join('')
  const dia = new Date(fecha).toLocaleDateString('es-AR', { day: '2-digit', month: 'long', year: 'numeric' })
  const html = `<!doctype html><html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Presupuesto ${esc(nombre || 'Armá tu PC')} — Tecnópolis Olavarría</title>
<style>
  body { font-family: -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; color: #16181d; margin: 32px; }
  header { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; border-bottom: 2px solid #0077b3; padding-bottom: 12px; }
  header img { height: 53px; width: auto; }
  h1 { font-size: 20px; margin: 22px 0 4px; }
  .meta { color: #555; font-size: 13px; margin: 0 0 18px; }
  table { width: 100%; border-collapse: collapse; font-size: 13px; }
  th, td { text-align: left; padding: 8px 6px; border-bottom: 1px solid #ddd; vertical-align: top; }
  th { background: #f2f5f8; font-size: 12px; text-transform: uppercase; letter-spacing: .03em; }
  .n { text-align: right; white-space: nowrap; }
  tfoot td { font-weight: 700; font-size: 15px; border-bottom: 0; padding-top: 14px; }
  .legal { color: #666; font-size: 11px; margin-top: 26px; line-height: 1.5; }
  .acciones { margin-top: 24px; }
  .acciones button { padding: 10px 18px; border-radius: 999px; border: 0; background: #0077b3; color: #fff; font-weight: 700; font-size: 15px; cursor: pointer; }
  @media print { body { margin: 12mm; } .acciones { display: none; } }
</style></head><body>
<header>
  <img src="${window.location.origin}/marca/tecnopolis-olavarria-logo-sobre-blanco.svg" alt="Tecnópolis Olavarría" width="212" height="53">
  <div style="text-align:right;font-size:12px;color:#555">tecnopolisolavarria.com</div>
</header>
<h1>Presupuesto · ${esc(nombre || 'Armá tu PC')}</h1>
<p class="meta">${esc(dia)}${cliente ? ` · ${esc(cliente)}` : ''}</p>
<table>
  <thead><tr><th>Componente</th><th>Producto</th><th class="n">Cant.</th><th class="n">Transferencia</th><th class="n">Dólares</th></tr></thead>
  <tbody>${filas}</tbody>
  <tfoot><tr><td colspan="3">Total</td><td class="n">$ ${formatPesos(totalArs)}</td><td class="n">US$ ${usd(totalUsd)}</td></tr></tfoot>
</table>
<p class="legal"><strong>Precios sin IVA</strong>, por transferencia bancaria o en dólares, vigentes al ${esc(dia)}. Stock sujeto a disponibilidad; el precio final se confirma al hacer el pedido.</p>
<div class="acciones"><button onclick="window.print()">Imprimir o guardar PDF</button></div>
<script>window.addEventListener('load', function () { setTimeout(function () { window.print() }, 300) })</script>
</body></html>`
  const w = window.open('', '_blank')
  if (!w) { alert('Tu navegador bloqueó la ventana del presupuesto. Permití las ventanas emergentes e intentá de nuevo.'); return }
  w.document.open()
  w.document.write(html)
  w.document.close()
}
