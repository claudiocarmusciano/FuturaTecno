/*
 * Tecnito: la mascota del chat. Un robotito en SVG que se sienta sobre la burbuja del chat de
 * Forja, abajo a la derecha. Vive en la página y NO dentro del widget: así una actualización de
 * Forja no la borra. Todo discreto: flota, parpadea, mueve la antena y saluda cada tanto.
 *
 * - Tocarlo (o Enter/Espacio) abre el chat, haciendo clic en la burbuja real del widget.
 * - Se esconde mientras el chat está abierto (el panel del widget lleva la clase "abierto").
 * - El globito "¡Hola! Soy Tecnito 👋" aparece una sola vez por visita (sessionStorage).
 * - Con "reducir movimiento" activado en el sistema, queda quieto.
 * Si el widget no carga, Tecnito no aparece: sin chat no hay nada que abrir.
 */
(function () {
  if (window.__tecnito) return;
  window.__tecnito = true;

  var ROBOT =
    '<g class="tec-antena"><line x1="32" y1="14" x2="32" y2="5" stroke="#9aa0ab" stroke-width="2.4" stroke-linecap="round"/>' +
    '<circle class="tec-luz" cx="32" cy="4.5" r="3.6" fill="#00aeef"/></g>' +
    '<rect x="6" y="24" width="5" height="12" rx="2.5" fill="#00aeef"/>' +
    '<rect x="53" y="24" width="5" height="12" rx="2.5" fill="#00aeef"/>' +
    '<rect x="10" y="13" width="44" height="34" rx="12" fill="#231f20" stroke="#00aeef" stroke-width="2.2"/>' +
    '<rect x="16" y="20" width="32" height="18" rx="8" fill="#101216"/>' +
    '<ellipse class="tec-ojo" cx="25" cy="28.5" rx="3.4" ry="4.2" fill="#00aeef"/>' +
    '<ellipse class="tec-ojo" cx="39" cy="28.5" rx="3.4" ry="4.2" fill="#00aeef"/>' +
    '<path d="M27.5 34.2 Q32 37 36.5 34.2" fill="none" stroke="#00aeef" stroke-width="1.8" stroke-linecap="round"/>' +
    '<rect x="19" y="48" width="26" height="19" rx="7" fill="#231f20" stroke="#00aeef" stroke-width="2"/>' +
    '<circle cx="32" cy="57.5" r="3.4" fill="#ffffff"/>' +
    '<rect x="11" y="50" width="7" height="13" rx="3.5" fill="#9aa0ab"/>' +
    '<g class="tec-brazo"><rect x="46" y="50" width="7" height="13" rx="3.5" fill="#9aa0ab"/></g>';

  var CSS =
    '.tecnito{position:fixed;right:24px;bottom:84px;width:50px;height:58px;cursor:pointer;z-index:2147483000;' +
    'filter:drop-shadow(0 4px 8px rgba(0,0,0,.35));animation:tec-flota 3.2s ease-in-out infinite;transition:opacity .25s,transform .25s}' +
    '.tecnito svg{width:100%;height:100%;overflow:visible;display:block}' +
    '.tecnito:focus-visible{outline:2px solid #00aeef;outline-offset:4px;border-radius:12px}' +
    '.tecnito.oculto,.tec-globo.oculto{opacity:0;pointer-events:none;transform:translateY(8px)}' +
    '.tec-antena{transform-origin:32px 14px;animation:tec-antena 3.2s ease-in-out infinite}' +
    '.tec-luz{animation:tec-luz 2.4s ease-in-out infinite}' +
    '.tec-ojo{transform-box:fill-box;transform-origin:center;animation:tec-parpadeo 4.6s infinite}' +
    '.tec-brazo{transform-origin:50px 44px;animation:tec-saludo 9s ease-in-out infinite}' +
    '.tec-globo{position:fixed;right:82px;bottom:104px;z-index:2147483000;background:#fff;color:#231f20;' +
    'font:600 13px/1.3 -apple-system,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;padding:7px 11px;' +
    'border-radius:12px 12px 3px 12px;box-shadow:0 4px 14px rgba(0,0,0,.3);white-space:nowrap;' +
    'opacity:0;transform:translateY(6px);transition:opacity .35s,transform .35s;pointer-events:none}' +
    '.tec-globo.visible{opacity:1;transform:none}' +
    '@keyframes tec-flota{0%,100%{translate:0 0}50%{translate:0 -4px}}' +
    '@keyframes tec-antena{0%,100%{transform:rotate(-6deg)}50%{transform:rotate(6deg)}}' +
    '@keyframes tec-luz{0%,100%{opacity:1}50%{opacity:.45}}' +
    '@keyframes tec-parpadeo{0%,92%,100%{transform:scaleY(1)}95%{transform:scaleY(.1)}}' +
    '@keyframes tec-saludo{0%,78%,100%{transform:rotate(0)}82%{transform:rotate(-28deg)}86%{transform:rotate(-8deg)}90%{transform:rotate(-28deg)}94%{transform:rotate(0)}}' +
    '@media (prefers-reduced-motion:reduce){.tecnito,.tecnito *{animation:none!important}}' +
    '@media (max-width:600px){.tecnito{width:40px;height:46px;right:29px;bottom:82px}.tec-globo{display:none}}';

  function iniciar(raiz) {
    var burbuja = raiz.querySelector('.burbuja');
    var panel = raiz.querySelector('.panel');
    if (!burbuja) return;

    var estilo = document.createElement('style');
    estilo.textContent = CSS;
    document.head.appendChild(estilo);

    var tecnito = document.createElement('div');
    tecnito.className = 'tecnito';
    tecnito.setAttribute('role', 'button');
    tecnito.setAttribute('tabindex', '0');
    tecnito.setAttribute('aria-label', 'Chatear con Tecnito');
    tecnito.title = 'Chateá con Tecnito';
    tecnito.innerHTML = '<svg viewBox="0 0 64 74" aria-hidden="true">' + ROBOT + '</svg>';
    document.body.appendChild(tecnito);

    var globo = document.createElement('div');
    globo.className = 'tec-globo';
    globo.setAttribute('aria-hidden', 'true');
    globo.textContent = '¡Hola! Soy Tecnito 👋';
    document.body.appendChild(globo);

    function abrirChat() { burbuja.click(); }
    tecnito.addEventListener('click', abrirChat);
    tecnito.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); abrirChat(); }
    });

    // Con el chat abierto, Tecnito se aparta: el panel ocupa ese lugar.
    function sincronizar() {
      var abierto = !!(panel && panel.classList.contains('abierto'));
      tecnito.classList.toggle('oculto', abierto);
      globo.classList.toggle('oculto', abierto);
      if (abierto) globo.classList.remove('visible');
    }
    if (panel) new MutationObserver(sincronizar).observe(panel, { attributes: true, attributeFilter: ['class'] });
    sincronizar();

    // El saludo, una sola vez por visita.
    try {
      if (!sessionStorage.getItem('tecnito-saludo')) {
        sessionStorage.setItem('tecnito-saludo', '1');
        setTimeout(function () {
          if (tecnito.classList.contains('oculto')) return;
          globo.classList.add('visible');
          setTimeout(function () { globo.classList.remove('visible'); }, 5000);
        }, 2500);
      }
    } catch (e) { /* sin sessionStorage (modo privado estricto): sin globito, y listo */ }
  }

  // El widget de Forja se arma solo y tarda un poco: se lo espera hasta 15 s.
  var intentos = 0;
  (function esperar() {
    var host = document.querySelector('[data-forja-widget]');
    var raiz = host && host.shadowRoot;
    if (raiz && raiz.querySelector('.burbuja')) return iniciar(raiz);
    if (++intentos < 50) setTimeout(esperar, 300);
  })();
})();
