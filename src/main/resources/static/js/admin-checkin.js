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

      // ESTILOS FORZADOS CON !IMPORTANT
      const estiloTarjeta = "display: flex !important; align-items: center !important; gap: 12px !important; padding: 12px 16px !important; background-color: #f8fafc !important; border: 1px solid #e2e8f0 !important; border-radius: 10px !important; margin-bottom: 8px !important; cursor: pointer !important; box-sizing: border-box !important; width: 100% !important;";
      const estiloCheckboxFalso = "width: 18px !important; height: 18px !important; min-width: 18px !important; border: 2px solid #94a3b8 !important; border-radius: 4px !important; background: #fff !important; display: block !important; flex-shrink: 0 !important; margin: 0 !important;";
      const estiloInfo = "display: flex !important; flex-direction: column !important; flex: 1 !important; gap: 2px !important;";
      const estiloNombre = "font-size: 14px !important; font-weight: 600 !important; color: #0f172a !important; margin: 0 !important; padding: 0 !important; line-height: 1.2 !important;";
      const estiloSub = "font-size: 12px !important; color: #64748b !important; margin: 0 !important; padding: 0 !important; line-height: 1.2 !important;";
      const estiloAvatar = "width: 32px !important; height: 32px !important; min-width: 32px !important; background-color: #0f172a !important; color: #fff !important; border-radius: 50% !important; display: flex !important; align-items: center !important; justify-content: center !important; font-size: 11px !important; font-weight: 800 !important; flex-shrink: 0 !important; margin: 0 !important;";
      const estiloEmoji = "font-size: 20px !important; flex-shrink: 0 !important; margin: 0 !important; width: 32px !important; text-align: center !important;";

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

  const cuadro = tarjeta.querySelector('.caja-checkbox-cuadro');
  const icono = tarjeta.querySelector('.icono-check-verde');
  
  if (chk.checked) {
    cuadro.style.backgroundColor = '#10b981';
    cuadro.style.borderColor = '#10b981';
    cuadro.innerHTML = '<span style="color:white; font-size:12px; position:absolute; top:50%; left:50%; transform:translate(-50%,-50%);">✓</span>';
    icono.style.display = 'block';
    itemsVerificados++;
  } else {
    cuadro.style.backgroundColor = '#fff';
    cuadro.style.borderColor = '#94a3b8';
    cuadro.innerHTML = '';
    icono.style.display = 'none';
    itemsVerificados--;
  }
  actualizarContador();
}

function actualizarContador() {
  document.getElementById('txtContadorVerificados').innerText = `${itemsVerificados}/${totalItemsVerificar} verificados`;
}

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
    cerrarModalCheckin();
    const tarjeta = document.getElementById(`tarjeta-reserva-${reservaActualId}`);
    if (tarjeta) tarjeta.remove();

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