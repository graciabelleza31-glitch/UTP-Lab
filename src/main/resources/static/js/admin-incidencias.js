function ejecutarNotificarIncidencia(btn) {
  const idReserva = btn.getAttribute('data-id');
  const nombresEstudiantes = btn.getAttribute('data-estudiante') || 'los estudiantes';

  btn.innerText = "Enviando...";
  btn.disabled = true;

  fetch(`/admin/incidencias/notificar/${idReserva}`, {
    method: 'POST'
  })
  .then(res => res.json())
  .then(data => {
    // 1. Cambiar el badge a "Notificada"
    const badge = document.getElementById(`badge-inc-${idReserva}`);
    if (badge) {
        badge.className = 'badge-estado-inc badge-notificada';
        badge.innerText = 'Notificada';
    }

    // 2. Cambiar el botón a "Resolver"
    btn.className = 'btn-resolver-verde';
    btn.innerHTML = '<i data-lucide="check"></i> Resolver Todo';
    btn.disabled = false;
    btn.onclick = function() { ejecutarResolverIncidencia(btn); };

    // 3. Actualizar contadores
    const numAbiertas = document.getElementById('numAbiertas');
    const numNotif = document.getElementById('numNotificadas');
    if (numAbiertas) numAbiertas.innerText = Math.max(0, parseInt(numAbiertas.innerText) - 1);
    if (numNotif) numNotif.innerText = parseInt(numNotif.innerText) + 1;

    if (typeof lucide !== 'undefined') lucide.createIcons();

    // 4. Mostrar modal de aviso
    abrirModalAviso(
      'correo',
      'Notificación de Deuda Enviada',
      `Se envió el detalle de la sanción a <strong>${nombresEstudiantes}</strong>.`,
      true
    );
  })
  .catch(() => {
    alert("Error al enviar notificación.");
    btn.innerText = "Notificar a Todos";
    btn.disabled = false;
  });
}

function ejecutarResolverIncidencia(btn) {
  const idReserva = btn.getAttribute('data-id');

  btn.innerText = "Resolviendo...";
  btn.disabled = true;

  fetch(`/admin/incidencias/resolver/${idReserva}`, {
    method: 'POST'
  })
  .then(res => res.json())
  .then(() => {
    // 1. Actualizar el badge a "Resuelta"
    const badge = document.getElementById(`badge-inc-${idReserva}`);
    if (badge) {
        badge.className = 'badge-estado-inc badge-resuelta';
        badge.style.background = '#dcfce7';
        badge.style.color = '#15803d';
        badge.style.border = '1px solid #bbf7d0';
        badge.innerText = 'Resuelta';
    }

    // 2. Cambiar el botón a "Ver Resumen"
    btn.className = 'btn-verde-iniciar';
    btn.style.background = '#f1f5f9';
    btn.style.color = '#475569';
    btn.style.border = '1px solid #e2e8f0';
    btn.innerHTML = '<i data-lucide="file-text"></i> Ver Resumen';
    btn.disabled = false;
    btn.onclick = function() { abrirResumenIncidencia(idReserva); };

    // 3. Actualizar contadores
    const numNotif = document.getElementById('numNotificadas');
    const numResueltas = document.getElementById('numResueltas');
    if (numNotif) numNotif.innerText = Math.max(0, parseInt(numNotif.innerText) - 1);
    if (numResueltas) numResueltas.innerText = parseInt(numResueltas.innerText) + 1;

    if (typeof lucide !== 'undefined') lucide.createIcons();
  })
  .catch(() => {
    alert("Error al resolver la incidencia.");
    btn.innerText = "Resolver Todo";
    btn.disabled = false;
  });
}

function abrirModalAviso(tipo, titulo, cuerpoHtml, mostrarBannerBloqueo) {
  const modal = document.getElementById('modalAvisoIncidencia');
  const icono = document.getElementById('modalAvisoIcono');
  const tit = document.getElementById('modalAvisoTitulo');
  const cuerpo = document.getElementById('modalAvisoCuerpo');
  const banner = document.getElementById('modalAvisoBanner');

  if (tipo === 'correo') {
    icono.className = 'icono-circulo-modal icono-modal-correo';
    icono.innerHTML = '📧';
  } else {
    icono.className = 'icono-circulo-modal icono-modal-resuelto';
    icono.innerHTML = '✓';
  }

  tit.innerText = titulo;
  cuerpo.innerHTML = cuerpoHtml;
  banner.style.display = mostrarBannerBloqueo ? 'flex' : 'none';

  modal.style.display = 'flex';
  if (typeof lucide !== 'undefined') lucide.createIcons();
}

function cerrarModalAviso() {
  document.getElementById('modalAvisoIncidencia').style.display = 'none';
}

// Función para ver el resumen de una incidencia resuelta
function abrirResumenIncidencia(idReserva) {
    // Buscamos la tarjeta en el DOM
    const card = document.getElementById(`card-inc-${idReserva}`);
    if (!card) return;

    const item = card.querySelector('.nombre-item-danado').innerText;
    const motivo = card.querySelector('.motivo-desc-txt').innerText;
    const estudiantes = card.querySelectorAll('.val-meta-txt')[1].innerText;
    const costo = card.querySelector('.val-meta-costo').innerText;

    let html = `
        <div id="modalResumenIncidencia" style="position: fixed; inset: 0; background: rgba(15, 23, 42, 0.65); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: 99999;">
            <div style="background: #fff; border-radius: 16px; width: 90%; max-width: 440px; padding: 2rem;">
                <div style="display: flex; justify-content: space-between; margin-bottom: 1rem;">
                    <h3 style="margin: 0; font-size: 1.2rem; font-weight: 800;">Resumen de Incidencia</h3>
                    <button onclick="document.getElementById('modalResumenIncidencia').remove()" style="background: none; border: none; font-size: 1.2rem; cursor: pointer;">✕</button>
                </div>
                <div style="background: #f8fafc; border-radius: 10px; padding: 1.25rem; font-size: 0.88rem; color: #334155; line-height: 1.6;">
                    <strong>Item:</strong> ${item}<br><br>
                    <strong>Motivo:</strong> ${motivo}<br><br>
                    <strong>Estudiantes:</strong> ${estudiantes}<br><br>
                    <strong>Costo Total:</strong> <span style="color: #ef4444; font-weight: 700;">${costo}</span><br><br>
                    <strong>Estado:</strong> <span style="color: #059669; font-weight: 700;">RESUELTA</span>
                </div>
                <button onclick="document.getElementById('modalResumenIncidencia').remove()" style="width: 100%; background: #0f172a; color: #fff; border: none; padding: 0.75rem; border-radius: 8px; font-weight: 600; cursor: pointer; margin-top: 1.5rem;">Cerrar</button>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', html);
}
