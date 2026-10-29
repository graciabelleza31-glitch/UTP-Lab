let idSancionActual = '';
let montoActual = 'S/ 350.00';

document.addEventListener("DOMContentLoaded", function () {
  if (typeof lucide !== 'undefined') lucide.createIcons();
});

function abrirModalPago(btn) {
  idSancionActual = btn.getAttribute('data-id');
  const item = btn.getAttribute('data-item') || 'Material de Lab';
  montoActual = btn.getAttribute('data-monto') || 'S/ 350.00';

  document.getElementById('modalTituloInc').innerText = `Pago de Incidencia · ${item}`;
  document.getElementById('modalMontoPagar').innerText = montoActual;
  document.getElementById('labelComercioQr').innerText = `UTP Lab · ${montoActual}`;
  document.getElementById('btnConfirmarPago').innerText = `Confirmar pago · ${montoActual}`;

  // Mostrar paso 1, ocultar paso 2
  document.getElementById('pasoPagoSeleccion').style.display = 'block';
  document.getElementById('pasoPagoExitoso').style.display = 'none';
  document.getElementById('modalPagoIncidencia').style.display = 'flex';

  if (typeof lucide !== 'undefined') lucide.createIcons();
}

function cerrarModalPago() {
  document.getElementById('modalPagoIncidencia').style.display = 'none';
}

function seleccionarMetodo(elem, metodo) {
  document.querySelectorAll('.opcion-metodo').forEach(el => el.classList.remove('seleccionado'));
  elem.classList.add('seleccionado');

  const tituloQr = document.getElementById('tituloQrMetodo');
  if (metodo === 'yape') {
    tituloQr.innerText = 'Pagar con Yape';
    tituloQr.style.color = '#7c3aed';
  } else if (metodo === 'plin') {
    tituloQr.innerText = 'Pagar con Plin';
    tituloQr.style.color = '#059669';
  } else {
    tituloQr.innerText = 'Transferencia BCP / Interbank';
    tituloQr.style.color = '#0284c7';
  }
}

function ejecutarPagoBackend() {
  const btn = document.getElementById('btnConfirmarPago');
  btn.innerText = "Procesando pago...";
  btn.disabled = true;

  fetch(`/alumno/incidencias/pagar/${idSancionActual}`, {
    method: 'POST'
  })
  .then(res => res.json())
  .then(data => {
    // Pasar a pantalla de ¡Pago Exitoso! (Figma)
    document.getElementById('pasoPagoSeleccion').style.display = 'none';
    document.getElementById('subtextoExitoMonto').innerText = `${montoActual} procesados correctamente`;
    document.getElementById('textoTxnCode').innerText = data.transaccion || 'TXN-0560042900';
    document.getElementById('pasoPagoExitoso').style.display = 'flex';

    if (typeof lucide !== 'undefined') lucide.createIcons();
  })
  .catch(() => {
    alert("Error al procesar el pago. Por favor intenta de nuevo.");
    btn.innerText = `Confirmar pago · ${montoActual}`;
    btn.disabled = false;
  });
}

function finalizarPagoYRecargar() {
  // 1. Ocultar modal
  cerrarModalPago();

  // 2. Ocultar banner de bloqueo rojo
  const banner = document.getElementById('bannerBloqueoAlerta');
  if (banner) banner.style.display = 'none';

  // 3. Ocultar badges de alerta
  const badgeHeader = document.getElementById('badgeHeaderPendientes');
  if (badgeHeader) badgeHeader.style.display = 'none';
  const badgeCampana = document.getElementById('badgeCampana');
  if (badgeCampana) badgeCampana.style.display = 'none';
  const badgeMenu = document.getElementById('badgeMenuLateral');
  if (badgeMenu) badgeMenu.style.display = 'none';

  // 4. Cambiar tarjeta a "Pagada" y "✓ Pagado y regularizado"
  const badgeEstado = document.getElementById(`badge-estado-${idSancionActual}`);
  if (badgeEstado) {
    badgeEstado.className = 'badge-pagada-verde';
    badgeEstado.innerText = 'Pagada';
  }

  const iconoBox = document.getElementById(`icono-box-${idSancionActual}`);
  if (iconoBox) {
    iconoBox.innerHTML = '<i data-lucide="check-square" style="color: #10b981; width: 20px; height: 20px;"></i>';
  }

  const contenedorAccion = document.getElementById(`contenedor-accion-${idSancionActual}`);
  if (contenedorAccion) {
    contenedorAccion.innerHTML = '<span class="texto-regularizado-verde">✓ Pagado y regularizado</span>';
  }

  if (typeof lucide !== 'undefined') lucide.createIcons();
}

function abrirModalIncidencia(fila) {
    const idSancion = fila.getAttribute('data-id-sancion');
    if (!idSancion) return;

    document.getElementById('modalDetalleIncidencia').style.display = 'flex';
    document.getElementById('incId').innerText = idSancion;

    fetch(`/alumno/api/incidencia/${idSancion}`)
        .then(res => res.json())
        .then(data => {
            if (!data.encontrado) {
                alert('No se encontró la incidencia.');
                cerrarModalIncidencia();
                return;
            }

            document.getElementById('incMotivo').innerText = data.motivo;
            document.getElementById('incReserva').innerText = data.idReserva;
            document.getElementById('incFecha').innerText = data.fechaReserva;
            document.getElementById('incLab').innerText = data.laboratorio;
            document.getElementById('incCubiculo').innerText = data.cubiculo;
            document.getElementById('incHorario').innerText = data.horario;
            document.getElementById('incMonto').innerText = 'S/ ' + parseFloat(data.monto).toFixed(2);

            const badge = document.getElementById('incEstado');
            badge.innerText = data.estado;
            badge.className = 'badge-estado-recibo';
            if (data.estado === 'PAGADA' || data.estado === 'RESUELTA') badge.classList.add('confirmada');
            else if (data.estado === 'NOTIFICADA') badge.classList.add('enuso');
            else badge.classList.add('cancelada');

            if (data.estado === 'PAGADA' || data.estado === 'RESUELTA') {
                document.getElementById('filaMetodoPago').style.display = 'flex';
                document.getElementById('filaFechaPago').style.display = 'flex';
                document.getElementById('incMetodo').innerText = data.metodoPago ? data.metodoPago.replace('PAGO_', '') : 'YAPE';
// Después (formateado):
if (data.fechaPago) {
    const fecha = new Date(data.fechaPago);
    document.getElementById('incFechaPago').innerText = fecha.toLocaleDateString('es-PE', { 
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' 
    });
} else {
    document.getElementById('incFechaPago').innerText = '—';
}
            } else {
                document.getElementById('filaMetodoPago').style.display = 'none';
                document.getElementById('filaFechaPago').style.display = 'none';
            }

            const contInteg = document.getElementById('incIntegrantes');
            contInteg.innerHTML = '';
            if (data.integrantes) {
                data.integrantes.forEach(i => {
                    contInteg.insertAdjacentHTML('beforeend', `
                        <div class="item-integrante-recibo">
                            <div class="avatar-mini-recibo">${i.nombre.substring(0,2).toUpperCase()}</div>
                            <div>
                                <strong>${i.nombre}</strong>
                                <span>${i.codigo}</span>
                            </div>
                        </div>
                    `);
                });
            }
        });
}

function cerrarModalIncidencia() {
    document.getElementById('modalDetalleIncidencia').style.display = 'none';
}