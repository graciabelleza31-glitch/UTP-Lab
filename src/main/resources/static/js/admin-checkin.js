let reservaActualId = null;
let totalItemsVerificar = 0;
let itemsVerificados = 0;

function abrirModalCheckin(idReserva) {
  reservaActualId = idReserva;
  
  const contInteg = document.getElementById('listaCheckIntegrantes');
  const contInsum = document.getElementById('listaCheckInsumos');
  const contEpps = document.getElementById('listaCheckEpps');

  contInteg.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando integrantes...</span>';
  contInsum.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando insumos...</span>';
  contEpps.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando EPPs...</span>';

  document.getElementById('modalCheckin').style.display = 'flex';

  fetch(`/admin/checkin/api/${idReserva}`)
    .then(res => res.json())
    .then(data => {
      document.getElementById('modalLabTitulo').innerText = data.laboratorio;
      document.getElementById('modalLabSubtitulo').innerText = `${data.idReserva} · ${data.horario}`;

      totalItemsVerificar = 0;
      itemsVerificados = 0;

      // 1. Integrantes
      contInteg.innerHTML = '';
      data.integrantes.forEach(i => {
        totalItemsVerificar++;
        contInteg.insertAdjacentHTML('beforeend', `
          <div class="tarjeta-check-item" onclick="toggleVerificacion(this)">
            <input type="checkbox" class="check-oculto" />
            <div class="caja-checkbox-cuadro"></div>
            <div class="avatar-circulo-verif">${i.iniciales}</div>
            <div class="info-texto-verif">
              <strong class="nombre-persona-verif">${i.nombre}</strong>
              <span class="sub-codigo-verif">${i.codigo}</span>
            </div>
            <span class="icono-check-verde">✓</span>
          </div>
        `);
      });

      // 2. Insumos
      contInsum.innerHTML = '';
      if (!data.insumos || data.insumos.length === 0) {
        contInsum.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">No se solicitaron reactivos o insumos extra.</span>';
      } else {
        data.insumos.forEach(ins => {
          totalItemsVerificar++;
          contInsum.insertAdjacentHTML('beforeend', `
            <div class="tarjeta-check-item" onclick="toggleVerificacion(this)">
              <input type="checkbox" class="check-oculto" />
              <div class="caja-checkbox-cuadro"></div>
              <div class="info-texto-verif">
                <strong class="nombre-item-verif">${ins.nombre}</strong>
                <span class="sub-unidad-verif">${ins.unidad}</span>
              </div>
              <span class="icono-check-verde">✓</span>
            </div>
          `);
        });
      }

      // 3. EPPs
      contEpps.innerHTML = '';
      data.epps.forEach(epp => {
        totalItemsVerificar++;
        contEpps.insertAdjacentHTML('beforeend', `
          <div class="tarjeta-check-item" onclick="toggleVerificacion(this)">
            <input type="checkbox" class="check-oculto" />
            <div class="caja-checkbox-cuadro"></div>
            <span class="emoji-epp-icon">${epp.emoji}</span>
            <div class="info-texto-verif">
              <strong class="nombre-item-verif">${epp.nombre}</strong>
            </div>
            <span class="icono-check-verde">✓</span>
          </div>
        `);
      });

      actualizarContador();
      if (typeof lucide !== 'undefined') lucide.createIcons();
    })
    .catch(() => {
      alert('Error al cargar los detalles de la reserva.');
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