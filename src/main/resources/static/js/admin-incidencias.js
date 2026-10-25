// 1. NOTIFICAR A TODO EL GRUPO
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
    // Actualizar visualmente la tarjeta
    const iconoDiv = document.getElementById(`icono-inc-${idReserva}`);
    if (iconoDiv) iconoDiv.innerHTML = '<span>📧</span>';

    const badge = document.getElementById(`badge-inc-${idReserva}`);
    if (badge) {
      badge.className = 'badge-estado-inc badge-notificada';
      badge.innerText = 'Notificada';
    }

    const footer = document.getElementById(`footer-notif-${idReserva}`);
    if (footer) {
      footer.style.display = 'flex';
      footer.querySelector('.txt-fecha-envio').innerText = `Notificado: ${data.fechaNotificado}`;
    }

    // El botón pasa ahora a ser "Resolver"
    btn.className = 'btn-resolver-verde';
    btn.innerHTML = '<i data-lucide="check"></i> Resolver Todo';
    btn.disabled = false;
    btn.onclick = function() { ejecutarResolverIncidencia(btn); };

    // Actualizar contadores globales
    const numAbiertas = document.getElementById('numAbiertas');
    const numNotif = document.getElementById('numNotificadas');
    if (numAbiertas) numAbiertas.innerText = Math.max(0, parseInt(numAbiertas.innerText) - 1);
    if (numNotif) numNotif.innerText = parseInt(numNotif.innerText) + 1;

    if (typeof lucide !== 'undefined') lucide.createIcons();

    // Desplegar el modal explicativo
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

// 2. RESOLVER TODO EL GRUPO
function ejecutarResolverIncidencia(btn) {
  const idReserva = btn.getAttribute('data-id');
  const nombresEstudiantes = btn.getAttribute('data-estudiante') || 'los estudiantes';

  btn.innerText = "Resolviendo...";
  btn.disabled = true;

  fetch(`/admin/incidencias/resolver/${idReserva}`, {
    method: 'POST'
  })
  .then(res => res.json())
  .then(() => {
    // Desvanecer la tarjeta
    const card = document.getElementById(`card-inc-${idReserva}`);
    if (card) {
      card.style.transition = 'all 0.3s ease';
      card.style.opacity = '0';
      card.style.transform = 'translateY(-10px)';
      setTimeout(() => card.remove(), 300);
    }

    // Actualizar contadores globales
    const numNotif = document.getElementById('numNotificadas');
    const numResueltas = document.getElementById('numResueltas');
    const badgeMenu = document.getElementById('badgeMenuIncidencias');

    if (numNotif) numNotif.innerText = Math.max(0, parseInt(numNotif.innerText) - 1);
    if (numResueltas) numResueltas.innerText = parseInt(numResueltas.innerText) + 1;
    if (badgeMenu) badgeMenu.innerText = Math.max(0, parseInt(badgeMenu.innerText) - 1);

    // Desplegar modal de confirmación
    abrirModalAviso(
      'resuelto',
      'Pago y Sanción Resuelta',
      `Se registró el pago de <strong>${nombresEstudiantes}</strong>. La sanción ha sido levantada y los estudiantes quedan <strong>habilitados</strong>.`,
      false
    );
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