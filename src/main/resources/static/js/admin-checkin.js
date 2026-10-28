let reservaActualId = null;
let totalItemsVerificar = 0;
let itemsVerificados = 0;

function abrirModalCheckin(idReserva) {
  reservaActualId = idReserva;
  
  const contInteg = document.getElementById('listaCheckIntegrantes');
  const contInsum = document.getElementById('listaCheckInsumos');
  const contEpps = document.getElementById('listaCheckEpps');

  contInteg.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando...</span>';
  contInsum.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando...</span>';
  contEpps.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando...</span>';

  document.getElementById('modalCheckin').style.display = 'flex';

  fetch(`/admin/checkin/api/${idReserva}`)
    .then(res => res.json())
    .then(data => {
      document.getElementById('modalLabTitulo').innerText = data.laboratorio;
      document.getElementById('modalLabSubtitulo').innerText = `${data.idReserva} · ${data.horario}`;

      totalItemsVerificar = 0;
      itemsVerificados = 0;

      // Estilos en línea para forzar el diseño
      const estiloTarjeta = "display: flex; align-items: center; gap: 10px; padding: 12px 16px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; margin-bottom: 8px; cursor: pointer;";
      const estiloCheckboxFalso = "width: 18px; height: 18px; border: 2px solid #94a3b8; border-radius: 4px; background: #fff; flex-shrink: 0; display: block;";
      const estiloInfo = "display: flex; flex-direction: column;";
      const estiloNombre = "font-size: 14px; font-weight: 600; color: #0f172a;";
      const estiloSub = "font-size: 12px; color: #64748b;";
      const estiloAvatar = "width: 32px; height: 32px; background-color: #0f172a; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 800; flex-shrink: 0;";
      const estiloEmoji = "font-size: 20px; flex-shrink: 0;";

      // 1. Integrantes
      contInteg.innerHTML = '';
      data.integrantes.forEach(i => {
        totalItemsVerificar++;
        contInteg.insertAdjacentHTML('beforeend', `
          <div class="tarjeta-check-item" style="${estiloTarjeta}" onclick="toggleVerificacion(this)">
            <input type="checkbox" style="display: none !important;" class="check-oculto" />
            <div class="caja-checkbox-cuadro" style="${estiloCheckboxFalso}"></div>
            <div class="avatar-circulo-verif" style="${estiloAvatar}">${i.iniciales}</div>
            <div class="info-texto-verif" style="${estiloInfo}">
              <strong class="nombre-persona-verif" style="${estiloNombre}">${i.nombre}</strong>
              <span class="sub-codigo-verif" style="${estiloSub}">${i.codigo}</span>
            </div>
            <span class="icono-check-verde" style="color: #10b981; font-weight: 900; font-size: 16px; display: none;">✓</span>
          </div>
        `);
      });

      // 2. Insumos
      contInsum.innerHTML = '';
      if (!data.insumos || data.insumos.length === 0) {
        contInsum.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">No se solicitaron insumos.</span>';
      } else {
        data.insumos.forEach(ins => {
          totalItemsVerificar++;
          contInsum.insertAdjacentHTML('beforeend', `
            <div class="tarjeta-check-item" style="${estiloTarjeta}" onclick="toggleVerificacion(this)">
              <input type="checkbox" style="display: none !important;" class="check-oculto" />
              <div class="caja-checkbox-cuadro" style="${estiloCheckboxFalso}"></div>
              <div class="info-texto-verif" style="${estiloInfo}">
                <strong class="nombre-item-verif" style="${estiloNombre}">${ins.nombre}</strong>
                <span class="sub-unidad-verif" style="${estiloSub}">${ins.unidad}</span>
              </div>
              <span class="icono-check-verde" style="color: #10b981; font-weight: 900; font-size: 16px; display: none;">✓</span>
            </div>
          `);
        });
      }

      // 3. EPPs
      contEpps.innerHTML = '';
      data.epps.forEach(epp => {
        totalItemsVerificar++;
        contEpps.insertAdjacentHTML('beforeend', `
          <div class="tarjeta-check-item" style="${estiloTarjeta}" onclick="toggleVerificacion(this)">
            <input type="checkbox" style="display: none !important;" class="check-oculto" />
            <div class="caja-checkbox-cuadro" style="${estiloCheckboxFalso}"></div>
            <span class="emoji-epp-icon" style="${estiloEmoji}">${epp.emoji}</span>
            <div class="info-texto-verif" style="${estiloInfo}">
              <strong class="nombre-item-verif" style="${estiloNombre}">${epp.nombre}</strong>
            </div>
            <span class="icono-check-verde" style="color: #10b981; font-weight: 900; font-size: 16px; display: none;">✓</span>
          </div>
        `);
      });

      actualizarContador();
      if (typeof lucide !== 'undefined') lucide.createIcons();
    })
    .catch(() => {
      alert('Error al cargar los detalles.');
      cerrarModalCheckin();
    });
}

function cerrarModalCheckin() {
  document.getElementById('modalCheckin').style.display = 'none';
  reservaActualId = null;
}

function toggleVerificacion(tarjeta) {
  const chk = tarjeta.querySelector('.check-oculto');
  chk.checked = !chk.checked;
  tarjeta.classList.toggle('marcado', chk.checked);

  if (chk.checked) {
    itemsVerificados++;
  } else {
    itemsVerificados--;
  }
  actualizarContador();
}

function actualizarContador() {
  document.getElementById('txtContadorVerificados').innerText = `${itemsVerificados}/${totalItemsVerificar} verificados`;
}

// CONFIRMACIÓN ASÍNCRONA (AJAX) - SIN REDIRECCIÓN
function enviarConfirmacionCheckin() {
  if (!reservaActualId) return;

  const btn = document.getElementById('btnConfirmarCheckin');
  btn.innerText = "Procesando...";
  btn.disabled = true;

  fetch(`/admin/checkin/confirmar/${reservaActualId}`, {
    method: 'POST'
  })
  .then(res => {
    if (!res.ok) throw new Error("Error en servidor");
    return res.json();
  })
  .then(() => {
    // 1. Cerrar el modal
    cerrarModalCheckin();

    // 2. Retirar la tarjeta del DOM de inmediato
    const tarjeta = document.getElementById(`tarjeta-reserva-${reservaActualId}`);
    if (tarjeta) {
      tarjeta.remove();
    }

    // 3. Evaluar si ya no quedan reservas pendientes
    const contenedor = document.getElementById('contenedorListaCheckin');
    const restantes = contenedor ? contenedor.querySelectorAll('.tarjeta-reserva-checkin-fila').length : 0;
    
    if (restantes === 0) {
      if (contenedor) contenedor.style.display = 'none';
      document.getElementById('tarjetaSinCheckins').style.display = 'flex';
    }
  })
  .catch(() => {
    alert("Ocurrió un error al procesar el check-in.");
  })
  .finally(() => {
    btn.innerText = "Confirmar check-in →";
    btn.disabled = false;
  });
}