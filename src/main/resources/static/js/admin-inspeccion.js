let idReservaInspActual = null;
let listaItemsParaInspeccionar = [];

function abrirModalInspeccion(idReserva) {
  idReservaInspActual = idReserva;
  listaItemsParaInspeccionar = [];

  const contInsumos = document.getElementById('listaInspInsumos');
  const contEpps = document.getElementById('listaInspEpps');

  contInsumos.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando...</span>';
  contEpps.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">Cargando...</span>';

  document.getElementById('modalInspeccion').style.display = 'flex';

  fetch(`/admin/inspeccion/api/${idReserva}`)
    .then(res => res.json())
    .then(data => {
      document.getElementById('modalInspLabTitulo').innerText = data.laboratorio;
      document.getElementById('modalInspSubtitulo').innerText = `${data.idReserva} · ${data.horario}`;

      // 1. Insumos
      contInsumos.innerHTML = '';
      if (data.insumos.length === 0) {
        contInsumos.innerHTML = '<span style="color:#94a3b8; font-size:0.85rem;">No se prestaron materiales de inventario adicionales.</span>';
      } else {
        data.insumos.forEach(item => {
          listaItemsParaInspeccionar.push({ ...item, estado: 'conforme', motivo: '' });
          contInsumos.insertAdjacentHTML('beforeend', generarHtmlItem(item));
        });
      }

      // 2. EPPs Retornables
      contEpps.innerHTML = '';
      data.epps.forEach(epp => {
        listaItemsParaInspeccionar.push({ ...epp, estado: 'conforme', motivo: '' });
        contEpps.insertAdjacentHTML('beforeend', generarHtmlItem(epp));
      });

      evaluarEstadoGeneral();
      if (typeof lucide !== 'undefined') lucide.createIcons();
    })
    .catch(() => {
      mostrarModalResultado(false, "Error al cargar los datos de la reserva.");
      cerrarModalInspeccion();
    });
}

function generarHtmlItem(item) {
  return `
    <div class="tarjeta-item-inspeccionar" id="row-item-${item.id}">
      <div class="fila-principal-item-insp">
        <div style="display:flex; align-items:center; gap:0.6rem;">
          <span style="font-size: 1.25rem;">${item.emoji}</span>
          <div>
            <strong style="font-size:0.88rem; color:#0f172a; display:block;">${item.nombre}</strong>
            <span style="font-size:0.75rem; color:#64748b;">Reposición: S/ ${Number(item.costoReposicion).toFixed(2)}</span>
          </div>
        </div>
        <div class="acciones-estado-item">
          <button type="button" 
                  class="btn-estado-check activo-conforme" 
                  id="btn-conf-${item.id}" 
                  onclick="marcarEstado('${item.id}', 'conforme')">✓ Conforme</button>
          <button type="button" 
                  class="btn-estado-check" 
                  id="btn-dan-${item.id}" 
                  onclick="marcarEstado('${item.id}', 'danado')">✕ Dañado / Pérdida</button>
        </div>
      </div>
      <input type="text" 
             id="motivo-${item.id}" 
             class="input-motivo-dano" 
             placeholder="Describa el daño o pérdida (ej. se quebró completamente)..."
             oninput="actualizarMotivoTexto('${item.id}', this.value)" />
    </div>
  `;
}

function marcarEstado(id, nuevoEstado) {
  const it = listaItemsParaInspeccionar.find(x => String(x.id) === String(id));
  if (!it) return;

  it.estado = nuevoEstado;
  const fila = document.getElementById(`row-item-${id}`);
  const btnConf = document.getElementById(`btn-conf-${id}`);
  const btnDan = document.getElementById(`btn-dan-${id}`);
  const inputMotivo = document.getElementById(`motivo-${id}`);

  if (nuevoEstado === 'danado') {
    fila.classList.add('marcado-danado');
    btnConf.classList.remove('activo-conforme');
    btnDan.classList.add('activo-danado');
    inputMotivo.style.display = 'block';
    inputMotivo.focus();
  } else {
    fila.classList.remove('marcado-danado');
    btnConf.classList.add('activo-conforme');
    btnDan.classList.remove('activo-danado');
    inputMotivo.style.display = 'none';
    it.motivo = '';
  }

  evaluarEstadoGeneral();
}

function actualizarMotivoTexto(id, val) {
  const it = listaItemsParaInspeccionar.find(x => String(x.id) === String(id));
  if (it) it.motivo = val;
}

function evaluarEstadoGeneral() {
  const danados = listaItemsParaInspeccionar.filter(x => x.estado === 'danado');
  const txt = document.getElementById('txtAlertaDanio');
  const btn = document.getElementById('btnFinalizarInsp');

  if (danados.length > 0) {
    txt.style.color = '#dc2626';
    txt.innerText = `⚠️ ${danados.length} daño(s) detectado(s) · Pasará a Incidencias`;
    btn.style.backgroundColor = '#dc2626';
  } else {
    txt.style.color = '#059669';
    txt.innerText = '✓ Todos los materiales conformes';
    btn.style.backgroundColor = '#0d9488';
  }
}

function cerrarModalInspeccion() {
  document.getElementById('modalInspeccion').style.display = 'none';
  idReservaInspActual = null;
}

// ACCIÓN: FINALIZAR INSPECCIÓN
function enviarFinalizarInspeccion() {
  if (!idReservaInspActual) return;

  const reservaIdAEliminar = idReservaInspActual;

  const danados = listaItemsParaInspeccionar
    .filter(x => x.estado === 'danado')
    .map(x => ({
      idProducto: x.id,
      nombre: x.nombre,
      costoReposicion: x.costoReposicion,
      motivo: x.motivo && x.motivo.trim() !== '' ? x.motivo.trim() : `Daño en ${x.nombre} durante la práctica`
    }));

  const btn = document.getElementById('btnFinalizarInsp');
  btn.innerText = "Finalizando...";
  btn.disabled = true;

  fetch(`/admin/inspeccion/finalizar/${reservaIdAEliminar}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ danados: danados })
  })
  .then(res => {
    if (!res.ok) throw new Error("Error en servidor al procesar inspección");
    return res.json();
  })
  .then(data => {
    cerrarModalInspeccion();

    const tarjeta = document.getElementById(`tarjeta-inspeccion-${reservaIdAEliminar}`);
    if (tarjeta) {
      tarjeta.style.transition = 'all 0.3s ease';
      tarjeta.style.opacity = '0';
      tarjeta.style.transform = 'translateY(-10px)';
      setTimeout(() => {
        tarjeta.remove();
        
        const contenedor = document.getElementById('contenedorInspeccion');
        const restantes = contenedor ? contenedor.querySelectorAll('.tarjeta-reserva-checkin-fila').length : 0;
        if (restantes === 0) {
          if (contenedor) contenedor.style.display = 'none';
          document.getElementById('sinInspeccionesVacio').style.display = 'flex';
        }
      }, 300);
    }

    if (data.huboDanios) {
      mostrarModalResultado(
        true, 
        "Daño Registrado", 
        "La inspección finalizó reportando daños. Se generó la sanción y la reserva fue trasladada al Módulo de Incidencias."
      );
      const badgeMenu = document.getElementById('badgeMenuIncidencias');
      if (badgeMenu) badgeMenu.innerText = parseInt(badgeMenu.innerText || '0') + 1;
    } else {
      mostrarModalResultado(
        false, 
        "Inspección Conforme", 
        "El material fue devuelto en perfecto estado. El laboratorio y cubículo quedaron completamente liberados."
      );
    }
  })
  .catch(err => {
    alert("Ocurrió un error al registrar la inspección: " + err.message);
  })
  .finally(() => {
    btn.innerText = "Finalizar Inspección →";
    btn.disabled = false;
  });
}

function mostrarModalResultado(huboDanio, titulo, mensaje) {
  const modal = document.getElementById('modalResultado');
  const icono = document.getElementById('iconoResultado');
  const tit = document.getElementById('tituloResultado');
  const desc = document.getElementById('mensajeResultado');

  if (huboDanio) {
    icono.className = 'icono-resultado-circulo res-danio';
    icono.innerText = '⚠️';
  } else {
    icono.className = 'icono-resultado-circulo res-exito';
    icono.innerText = '✓';
  }

  tit.innerText = titulo;
  desc.innerText = mensaje;
  modal.style.display = 'flex';
}

function cerrarModalResultado() {
  document.getElementById('modalResultado').style.display = 'none';
}

// Función para registrar la salida (Check-out)
function abrirModalCheckout(idReserva) {
    if (!confirm('¿Registrar la salida del estudiante y liberar el laboratorio? El material pasará a estado DEVUELTO para su inspección.')) {
        return;
    }

    // Hacemos la petición al endpoint de checkin que ya tenías
    fetch(`/admin/checkin/confirmar/${idReserva}`, {
        method: 'POST'
    })
    .then(res => {
        if (!res.ok) throw new Error("Error al registrar checkout");
        return res.json();
    })
    .then(() => {
        alert("✅ Salida registrada. Ahora puedes inspeccionar el material.");
        window.location.reload(); // Recargamos para que el botón cambie a "Inspeccionar"
    })
    .catch(() => alert("Ocurrió un error al registrar la salida."));
}

// Función para registrar la salida (Check-out) desde la vista de Inspección
function abrirModalCheckout(idReserva) {
    if (!confirm('¿Registrar la salida del estudiante y liberar el laboratorio? El material pasará a estado DEVUELTO para su inspección.')) {
        return;
    }

    // Hacemos la petición al endpoint que ya tienes en tu CheckinController
    fetch(`/admin/checkin/confirmar/${idReserva}`, {
        method: 'POST'
    })
    .then(res => {
        if (!res.ok) throw new Error("Error al registrar checkout");
        return res.json();
    })
    .then(() => {
        alert("✅ Salida registrada. Ahora puedes inspeccionar el material.");
        window.location.reload(); // Recargamos para que el botón cambie a "Inspeccionar"
    })
    .catch(() => alert("Ocurrió un error al registrar la salida."));
}