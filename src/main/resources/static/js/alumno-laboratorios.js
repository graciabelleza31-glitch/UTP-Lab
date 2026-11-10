// ============================================================================
// 1. INICIALIZACIÓN DE ÍCONOS Y FECHAS
// ============================================================================
document.addEventListener("DOMContentLoaded", function () {
    if (typeof lucide !== 'undefined') {
        lucide.createIcons();
    }

    const hoy = new Date();
    const año = hoy.getFullYear();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    const fechaHoy = `${año}-${mes}-${dia}`;

    const inputConsulta = document.getElementById('inputFechaConsulta');
    const inputWizard = document.getElementById('wizardFechaInput');
    const inputHidden = document.getElementById('wizardFechaEnvio');

    if (inputConsulta) {
        inputConsulta.value = fechaHoy;
        inputConsulta.min = fechaHoy;
    }
    if (inputWizard) {
        inputWizard.value = fechaHoy;
        inputWizard.min = fechaHoy;
    }
    if (inputHidden) {
        inputHidden.value = fechaHoy;
    }
});

const BLOQUES_HORARIOS = [
    { h: "07:00", f: "08:00" }, { h: "08:00", f: "09:00" }, { h: "09:00", f: "10:00" },
    { h: "10:00", f: "11:00" }, { h: "11:00", f: "12:00" }, { h: "12:00", f: "13:00" },
    { h: "13:00", f: "14:00" }, { h: "14:00", f: "15:00" }, { h: "15:00", f: "16:00" },
    { h: "16:00", f: "17:00" }, { h: "17:00", f: "18:00" }, { h: "18:00", f: "19:00" }
];

// ============================================================================
// 2. FILTROS Y MODAL DE HORARIOS (CONSULTA)
// ============================================================================
function filtrarLabs(tipo, btn) {
    document.querySelectorAll('.barra-filtros .btn-filtro').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    document.querySelectorAll('.rejilla-laboratorios .tarjeta-lab-grid').forEach(t => {
        t.style.display = (tipo === 'TODOS' || t.getAttribute('data-tipo') === tipo) ? 'flex' : 'none';
    });
}

function gestionarClickLab(tarjeta) {
    if (tarjeta.classList.contains('tarjeta-deshabilitada')) return;
    abrirModalHorarios(tarjeta);
}

let labSeleccionadoActual = null;

function abrirModalHorarios(tarjeta) {
    labSeleccionadoActual = {
        id: tarjeta.getAttribute('data-id'),
        nombre: tarjeta.getAttribute('data-nombre'),
        cubiculo: tarjeta.getAttribute('data-cubiculo')
    };
    document.getElementById('tituloLabModal').innerText = labSeleccionadoActual.nombre;
    document.getElementById('subtituloLabModal').innerText = `Bloque ${labSeleccionadoActual.cubiculo} · 12 puestos`;
    document.getElementById('modalHorarios').style.display = 'flex';
    actualizarHorariosModalConsulta();
    if (typeof lucide !== 'undefined') lucide.createIcons();
}

function cerrarModalHorarios() {
    document.getElementById('modalHorarios').style.display = 'none';
}

function actualizarHorariosModalConsulta() {
    if (!labSeleccionadoActual) return;
    const fecha = document.getElementById('inputFechaConsulta').value;
    const grid = document.getElementById('gridHorariosConsulta');
    grid.innerHTML = '<span style="grid-column: 1/-1; text-align: center; color: #94a3b8;">Cargando horarios...</span>';

    const ahora = new Date();
    const horaActual = ahora.getHours();
    const minutoActual = ahora.getMinutes();
    const hoyStr = ahora.toISOString().split('T')[0];
    const esHoy = (fecha === hoyStr);

    fetch(`/api/laboratorios/${labSeleccionadoActual.id}/horarios-ocupados?fecha=${fecha}`)
        .then(res => res.json())
        .then(ocupados => {
            grid.innerHTML = '';
            let cDisp = 0, cOcup = 0;
            BLOQUES_HORARIOS.forEach(b => {
                const horaBloque = parseInt(b.h.split(':')[0], 10);
                let yaPaso = false;
                if (esHoy) {
                    if (horaBloque < horaActual) yaPaso = true;
                    else if (horaBloque === horaActual && minutoActual >= 30) yaPaso = true;
                }
                const estaOcupado = ocupados.includes(b.h);
                const estaBloqueado = estaOcupado || yaPaso;

                const slot = document.createElement('div');
                slot.className = `btn-hora-slot ${estaBloqueado ? 'ocupado' : 'disponible'}`;
                slot.innerText = b.h;
                grid.appendChild(slot);

                if (estaBloqueado) cOcup++; else cDisp++;
            });
            document.getElementById('txtCantDisp').innerText = cDisp;
            document.getElementById('txtCantOcup').innerText = cOcup;
        })
        .catch(() => {
            grid.innerHTML = '';
            BLOQUES_HORARIOS.forEach(b => {
                const slot = document.createElement('div');
                slot.className = 'btn-hora-slot disponible';
                slot.innerText = b.h;
                grid.appendChild(slot);
            });
        });
}

function validarFechaNoPasada(input) {
    const hoy = new Date().toISOString().split('T')[0];
    if (input.value < hoy) {
        input.value = hoy;
        alert('No puedes seleccionar una fecha anterior a hoy.');
    }
}

function pasarDeHorariosAWizard() {
    cerrarModalHorarios();
    abrirWizardPaso1();
    if (labSeleccionadoActual) {
        const item = document.querySelector(`.item-lab-radio[data-id="${labSeleccionadoActual.id}"]`);
        if (item) seleccionarLabWizard(item);
    }
}

// ============================================================================
// 3. WIZARD DE RESERVA - ESTADO Y NAVEGACIÓN
// ============================================================================
let pasoActual = 1;
let tieneInsumosActivo = false;
let horasSeleccionadas = [];
let totalIntegrantes = 1;
const codigosAgregados = new Set();

function abrirWizardPaso1() {
    document.getElementById('modalWizard').style.display = 'flex';
    irALaboratorio();
    if (typeof lucide !== 'undefined') lucide.createIcons();
}

function cerrarWizard() {
    document.getElementById('modalWizard').style.display = 'none';
    document.getElementById('wizardIdLab').value = '';
    document.getElementById('wizardHoraInicio').value = '';
    document.getElementById('wizardHoraFin').value = '';
    document.querySelectorAll('.item-lab-radio').forEach(i => i.classList.remove('seleccionado'));

    horasSeleccionadas = [];
    document.querySelectorAll('.btn-slot-select').forEach(s => s.classList.remove('seleccionado'));
    document.getElementById('tagMax2Horas').style.display = 'none';

    document.querySelectorAll('.tarjeta-integrante-fila').forEach((f, idx) => {
        if (idx > 0) f.remove();
    });
    codigosAgregados.clear();
    totalIntegrantes = 1;
    document.getElementById('contadorBadgeIntegrantes').innerText = '1/5';
    document.getElementById('inputCodigoBuscar').value = '';
    document.getElementById('inputNombreAutocompletado').value = '';
    document.getElementById('msgErrorBusqueda').style.display = 'none';

    document.getElementById('checkSolicitarInsumos').checked = false;
    tieneInsumosActivo = false;
    document.querySelectorAll('#listaInsumosContenedor input[type="checkbox"]').forEach(c => c.checked = false);
    document.getElementById('contadorInsumosBadge').innerText = '0/10';

    document.querySelectorAll('.card-epp-select:not(.incluido)').forEach(c => {
        c.classList.remove('seleccionado');
        c.querySelector('input[type="checkbox"]').checked = false;
    });
    costoEppsTotal = 0;

    document.getElementById('checkAceptoTerminos').checked = false;
    irALaboratorio();
}

function toggleInsumosInmediato(checkbox) {
    tieneInsumosActivo = checkbox.checked;
    actualizarIndicadores(pasoActual);
}

function actualizarIndicadores(paso) {
    const totalPasos = tieneInsumosActivo ? 5 : 4;
    let html = '';
    for (let i = 1; i <= totalPasos; i++) {
        let clase = 'paso-circulo';
        let texto = i;
        if (i < paso) { clase += ' completado'; texto = '✓'; }
        else if (i === paso) { clase += ' activo'; }
        html += `<span class="${clase}">${texto}</span>`;
        if (i < totalPasos) html += `<span class="linea-paso"></span>`;
    }
    html += `<span class="txt-paso-contador">Paso ${paso}/${totalPasos}</span>`;
    document.getElementById('contenedorIndicadoresPasos').innerHTML = html;
}

function seleccionarLabWizard(item) {
    document.querySelectorAll('.item-lab-radio').forEach(i => i.classList.remove('seleccionado'));
    item.classList.add('seleccionado');
    document.getElementById('wizardIdLab').value = item.getAttribute('data-id');
    labSeleccionadoActual = {
        id: item.getAttribute('data-id'),
        nombre: item.getAttribute('data-nombre'),
        cubiculo: item.getAttribute('data-cubiculo')
    };
    cargarHorariosWizard();
}

function cambiarFechaWizard(val) {
    document.getElementById('wizardFechaEnvio').value = val;
    cargarHorariosWizard();
}

function filtrarLabsWizard(tipo, btn) {
    document.querySelectorAll('.barra-filtros.mini .btn-filtro').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    document.querySelectorAll('.item-lab-radio').forEach(item => {
        item.style.display = (tipo === 'TODOS' || item.getAttribute('data-tipo') === tipo) ? 'flex' : 'none';
    });
}

function cargarHorariosWizard() {
    const idLab = document.getElementById('wizardIdLab').value;
    const fecha = document.getElementById('wizardFechaEnvio').value;
    const grid = document.getElementById('gridHorasWizard');
    horasSeleccionadas = [];
    document.getElementById('tagMax2Horas').style.display = 'none';

    if (!idLab) {
        grid.innerHTML = '<span style="grid-column: 1/-1; text-align: center; color: #94a3b8;">Selecciona un laboratorio arriba para ver horarios</span>';
        return;
    }

    const ahora = new Date();
    const horaActual = ahora.getHours();
    const minutoActual = ahora.getMinutes();
    const hoyStr = ahora.toISOString().split('T')[0];
    const esHoy = (fecha === hoyStr);

    fetch(`/api/laboratorios/${idLab}/horarios-ocupados?fecha=${fecha}`)
        .then(res => res.json())
        .then(ocupados => {
            grid.innerHTML = '';
            BLOQUES_HORARIOS.forEach(b => {
                const horaBloque = parseInt(b.h.split(':')[0], 10);
                let yaPaso = false;
                if (esHoy) {
                    if (horaBloque < horaActual) yaPaso = true;
                    else if (horaBloque === horaActual && minutoActual >= 30) yaPaso = true;
                }
                const estaOcupado = ocupados.includes(b.h);
                const estaBloqueado = estaOcupado || yaPaso;
                const slot = document.createElement('div');
                slot.className = `btn-slot-select ${estaBloqueado ? 'bloqueado' : ''}`;
                slot.innerText = b.h;
                if (yaPaso) slot.title = 'Esta hora ya pasó';
                else if (estaOcupado) slot.title = 'Horario ocupado';
                if (!estaBloqueado) { slot.onclick = () => seleccionarHora(slot, b.h, b.f); }
                grid.appendChild(slot);
            });
        })
        .catch(() => {
            grid.innerHTML = '';
            BLOQUES_HORARIOS.forEach(b => {
                const slot = document.createElement('div');
                slot.className = 'btn-slot-select';
                slot.innerText = b.h;
                slot.onclick = () => seleccionarHora(slot, b.h, b.f);
                grid.appendChild(slot);
            });
        });
}

function seleccionarHora(btn, inicio, fin) {
    if (btn.classList.contains('bloqueado')) return;
    if (btn.classList.contains('seleccionado')) {
        btn.classList.remove('seleccionado');
        horasSeleccionadas = horasSeleccionadas.filter(h => h.inicio !== inicio);
    } else {
        if (horasSeleccionadas.length >= 2) { alert('Solo puedes seleccionar un máximo de 2 horas.'); return; }
        btn.classList.add('seleccionado');
        horasSeleccionadas.push({ inicio: inicio, fin: fin });
    }
    document.getElementById('tagMax2Horas').style.display = (horasSeleccionadas.length === 2) ? 'inline-block' : 'none';
    if (horasSeleccionadas.length > 0) {
        horasSeleccionadas.sort((a, b) => a.inicio.localeCompare(b.inicio));
        document.getElementById('wizardHoraInicio').value = horasSeleccionadas[0].inicio;
        document.getElementById('wizardHoraFin').value = horasSeleccionadas[horasSeleccionadas.length - 1].fin;
    } else {
        document.getElementById('wizardHoraInicio').value = '';
        document.getElementById('wizardHoraFin').value = '';
    }
}

function ocultarTodasSecciones() {
    document.getElementById('seccionPasoLab').style.display = 'none';
    document.getElementById('seccionPasoIntegrantes').style.display = 'none';
    document.getElementById('seccionPasoInsumos').style.display = 'none';
    document.getElementById('seccionPasoEpps').style.display = 'none';
    document.getElementById('seccionPasoBoleta').style.display = 'none';
}

function irALaboratorio() {
    pasoActual = 1; ocultarTodasSecciones();
    document.getElementById('seccionPasoLab').style.display = 'block';
    actualizarIndicadores(1);
}

function validarEIrAIntegrantes() {
    const idLab = document.getElementById('wizardIdLab').value;
    const hora = document.getElementById('wizardHoraInicio').value;
    if (!idLab) { alert('Por favor selecciona un laboratorio.'); return; }
    if (!hora) { alert('Por favor selecciona al menos un bloque horario.'); return; }
    irAIntegrantes();
}

function irAIntegrantes() {
    pasoActual = 2; ocultarTodasSecciones();
    document.getElementById('seccionPasoIntegrantes').style.display = 'block';
    actualizarIndicadores(2);
}

function validarIntegrantesYSiguiente() {
    tieneInsumosActivo = document.getElementById('checkSolicitarInsumos').checked;
    if (tieneInsumosActivo) { irAInsumos(); }
    else {
        document.querySelectorAll('#listaInsumosContenedor input[type="checkbox"]').forEach(c => c.checked = false);
        irAEPPs();
    }
}

function irAInsumos() {
    pasoActual = 3; ocultarTodasSecciones();
    document.getElementById('seccionPasoInsumos').style.display = 'block';
    actualizarIndicadores(3);
}

function volverDesdeEpps() {
    if (tieneInsumosActivo) irAInsumos(); else irAIntegrantes();
}

function irAEPPs() {
    pasoActual = tieneInsumosActivo ? 4 : 3; ocultarTodasSecciones();
    document.getElementById('seccionPasoEpps').style.display = 'block';
    actualizarIndicadores(pasoActual);
}

function irABoleta() {
    cargarBoletaResumen();
    pasoActual = tieneInsumosActivo ? 5 : 4;
    ocultarTodasSecciones();
    document.getElementById('seccionPasoBoleta').style.display = 'block';
    actualizarIndicadores(pasoActual);
}

// ============================================================================
// 4. INTEGRANTES - BÚSQUEDA Y AGREGAR
// ============================================================================
document.getElementById('inputCodigoBuscar')?.addEventListener('input', function () {
    const codigo = this.value.trim().toUpperCase();
    const msgError = document.getElementById('msgErrorBusqueda');
    const inputNombre = document.getElementById('inputNombreAutocompletado');

    if (codigo.length >= 6) {
        fetch('/api/alumnos/buscar/' + encodeURIComponent(codigo))
            .then(res => res.json())
            .then(data => {
                if (data.encontrado) {
                    if (!data.habilitado) {
                        msgError.innerText = data.motivo || 'El alumno no está habilitado para reservar.';
                        msgError.style.display = 'block';
                        inputNombre.value = '';
                        inputNombre.removeAttribute('data-codigo-real');
                    } else {
                        inputNombre.value = data.nombreCompleto;
                        inputNombre.setAttribute('data-codigo-real', data.codigo);
                        msgError.style.display = 'none';
                    }
                } else {
                    inputNombre.value = '';
                    msgError.innerText = 'Código o DNI no encontrado en el sistema.';
                    msgError.style.display = 'block';
                }
            })
            .catch(() => {
                inputNombre.value = '';
                msgError.innerText = 'Error al buscar el alumno.';
                msgError.style.display = 'block';
            });
    } else {
        inputNombre.value = '';
        msgError.style.display = 'none';
    }
});

function agregarIntegrantePorCodigo() {
    if (totalIntegrantes >= 5) { alert('Límite alcanzado: Máximo 5 integrantes por reserva.'); return; }
    const inputNombre = document.getElementById('inputNombreAutocompletado');
    const inputCodigo = document.getElementById('inputCodigoBuscar');
    const codigoReal = inputNombre.getAttribute('data-codigo-real') || inputCodigo.value.trim().toUpperCase();
    const nombre = inputNombre.value.trim();

    if (!codigoReal || !nombre) { alert('Ingresa un código o DNI registrado.'); return; }
    if (codigosAgregados.has(codigoReal)) { alert('Este integrante ya se encuentra en la lista.'); return; }

    const lista = document.getElementById('listaIntegrantesAgregados');
    const iniciales = nombre.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
    const html = `
        <div class="tarjeta-integrante-fila" id="integrante_${codigoReal}">
            <div class="avatar-circulo-estudiante">${iniciales}</div>
            <div class="info-estud-fila">
                <strong class="nombre-estud-txt">${nombre}</strong>
                <span class="codigo-estud-txt">${codigoReal}</span>
            </div>
            <button type="button" class="btn-eliminar-integrante" onclick="eliminarIntegrante('${codigoReal}')">✕</button>
            <input type="hidden" name="integrantesCodigos" value="${codigoReal}" />
        </div>
    `;
    lista.insertAdjacentHTML('beforeend', html);
    codigosAgregados.add(codigoReal);
    totalIntegrantes++;
    document.getElementById('contadorBadgeIntegrantes').innerText = `${totalIntegrantes}/5`;
    inputCodigo.value = '';
    inputNombre.value = '';
    inputNombre.removeAttribute('data-codigo-real');
}

function eliminarIntegrante(cod) {
    const f = document.getElementById('integrante_' + cod);
    if (f) {
        f.remove();
        codigosAgregados.delete(cod);
        totalIntegrantes--;
        document.getElementById('contadorBadgeIntegrantes').innerText = `${totalIntegrantes}/5`;
    }
}

// ============================================================================
// 5. INSUMOS - CONTADOR, BÚSQUEDA Y FILTRO
// ============================================================================
function actualizarContadorInsumos(chk) {
    const checks = document.querySelectorAll('#listaInsumosContenedor input[type="checkbox"]:checked');
    if (checks.length > 10) { alert('Máximo 10 insumos.'); chk.checked = false; return; }
    document.getElementById('contadorInsumosBadge').innerText = `${checks.length}/10`;
}

function buscarInsumos(txt) {
    const f = txt.toLowerCase();
    document.querySelectorAll('.item-insumo-check').forEach(i => {
        const nom = (i.getAttribute('data-nombre') || '').toLowerCase();
        i.style.display = nom.includes(f) ? 'block' : 'none';
    });
}

function filtrarCategoriaInsumos(cat, btn) {
    document.querySelectorAll('#seccionPasoInsumos .btn-filtro').forEach(b => b.classList.remove('activo'));
    btn.classList.add('activo');
    document.querySelectorAll('.item-insumo-check').forEach(i => {
        const c = (i.getAttribute('data-categoria') || '').toLowerCase();
        i.style.display = (cat === 'TODOS' || c.includes(cat.toLowerCase())) ? 'block' : 'none';
    });
}

// ============================================================================
// 6. EPPs Y COSTOS
// ============================================================================
let costoEppsTotal = 0;

function toggleEpp(card) {
    const chk = card.querySelector('input[type="checkbox"]');
    if (chk.disabled) return;
    chk.checked = !chk.checked;
    card.classList.toggle('seleccionado', chk.checked);
    calcularCostos();
}

function calcularCostos() {
    costoEppsTotal = 0;
    document.querySelectorAll('.card-epp-select input[type="checkbox"]:checked').forEach(c => {
        costoEppsTotal += parseFloat(c.getAttribute('data-precio') || 0);
    });
}

// ============================================================================
// 7. BOLETA RESUMEN
// ============================================================================
function cargarBoletaResumen() {
    calcularCostos();
    const lab = labSeleccionadoActual || { nombre: 'Laboratorio', cubiculo: 'S/C' };

    setTextIfExists('boletaLabCubiculo', `${lab.nombre} · ${lab.cubiculo}`);
    setTextIfExists('boletaFechaTxt', document.getElementById('wizardFechaEnvio').value);

    const hIni = document.getElementById('wizardHoraInicio').value || '08:00';
    const hFin = document.getElementById('wizardHoraFin').value || '10:00';
    setTextIfExists('boletaHorarioTxt', `${hIni} – ${hFin}`);

    // Integrantes
    const integrantesFilas = document.querySelectorAll('#listaIntegrantesAgregados .tarjeta-integrante-fila');
    let nombresIntegrantes = [];
    integrantesFilas.forEach(fila => {
        const nombre = fila.querySelector('.nombre-estud-txt')?.innerText || '';
        if (nombre) nombresIntegrantes.push(nombre);
    });
    setTextIfExists('boletaIntegrantesTitulo', `Integrantes (${nombresIntegrantes.length})`);
    setTextIfExists('boletaIntegrantesNombres', nombresIntegrantes.join(', '));

    // Insumos
    const insumos = document.querySelectorAll('#listaInsumosContenedor input[type="checkbox"]:checked');
    const bloqueInsumos = document.getElementById('boletaSeccionInsumosBloque');
    const listaUl = document.getElementById('boletaInsumosLista');
    if (listaUl) listaUl.innerHTML = '';
    if (insumos.length > 0 && tieneInsumosActivo) {
        if (bloqueInsumos) bloqueInsumos.style.display = 'block';
        setTextIfExists('boletaInsumosTitulo', `Insumos (${insumos.length})`);
        insumos.forEach(ins => {
            if (listaUl) {
                const li = document.createElement('li');
                li.innerText = ins.getAttribute('data-nombre');
                listaUl.appendChild(li);
            }
        });
    } else {
        if (bloqueInsumos) bloqueInsumos.style.display = 'none';
    }

    // EPPs / totales
    if (costoEppsTotal > 0) {
        setDisplayIfExists('filaCobroEpps', 'flex');
        setTextIfExists('boletaMontoEpps', `S/ ${costoEppsTotal.toFixed(2)}`);
        setTextIfExists('boletaMontoTotalTxt', `S/ ${costoEppsTotal.toFixed(2)}`);
        setValueIfExists('inputMontoTotalFinal', costoEppsTotal.toFixed(2));
        setTextIfExists('pasarelaTotalMonto', `S/ ${costoEppsTotal.toFixed(2)}`);
        // ⭐ Ya no existe montoQrText: lo cambiamos por montoYapeText
        setTextIfExists('montoYapeText', `S/ ${costoEppsTotal.toFixed(2)}`);
        setTextIfExists('btnAccionBoleta', "Ir a pagar →");
    } else {
        setDisplayIfExists('filaCobroEpps', 'none');
        setTextIfExists('boletaMontoTotalTxt', "S/ 0.00");
        setValueIfExists('inputMontoTotalFinal', "0.00");
        setTextIfExists('montoYapeText', "S/ 0.00");
        setTextIfExists('btnAccionBoleta', "Confirmar reserva →");
    }
}

// Helpers defensivos
function setTextIfExists(id, valor) {
    const el = document.getElementById(id);
    if (el) el.innerText = valor;
}
function setValueIfExists(id, valor) {
    const el = document.getElementById(id);
    if (el) el.value = valor;
}
function setDisplayIfExists(id, valor) {
    const el = document.getElementById(id);
    if (el) el.style.display = valor;
}


// ============================================================================
// 8. VALIDACIÓN Y ENVÍO (AJAX)
// ============================================================================
let enviandoReserva = false;
let ultimoIntentoPago = null;

async function validarYEnviarReserva() {
    if (enviandoReserva) return;
    const idLab = document.getElementById('wizardIdLab').value;
    const fecha = document.getElementById('wizardFechaEnvio').value;
    const inicio = document.getElementById('wizardHoraInicio').value;
    const fin = document.getElementById('wizardHoraFin').value;

    if (!idLab || !fecha || !inicio || !fin) {
        alert('Selecciona el laboratorio, la fecha y el horario.');
        return;
    }

    try {
        const respuesta = await fetch(
            `/api/laboratorios/${encodeURIComponent(idLab)}/horarios-ocupados?fecha=${encodeURIComponent(fecha)}`
        );
        if (!respuesta.ok) throw new Error('No se pudieron consultar los horarios.');
        const ocupados = await respuesta.json();

        const bloquesDelIntervalo = BLOQUES_HORARIOS.filter(b => b.h >= inicio && b.h < fin);
        const bloquesEnConflicto = bloquesDelIntervalo.filter(b => ocupados.includes(b.h));

        if (bloquesEnConflicto.length > 0) {
            alert('¡Cruce de horario!\n\nEl laboratorio ya tiene una reserva activa en el horario seleccionado. Elige otro horario antes de continuar.');
            cargarHorariosWizard();
            return;
        }

        enviandoReserva = true;

        const form = document.getElementById('formReservaCompleta');
        const formData = new FormData(form);

        ultimoIntentoPago = formData;
        ejecutarPagoAjax(formData);

    } catch (error) {
        console.error(error);
        alert('No pudimos verificar la disponibilidad del laboratorio. Por seguridad, la reserva no se ha enviado. Inténtalo nuevamente.');
    } finally {
        enviandoReserva = false;
    }
}

function procesarBotonFinalBoleta() {
    if (!document.getElementById('checkAceptoTerminos').checked) {
        alert('Debes aceptar los términos y condiciones para continuar.');
        return;
    }

    if (costoEppsTotal > 0) {
        document.getElementById('modalWizard').style.display = 'none';
        document.getElementById('modalPagoReserva').style.display = 'flex';
    } else {
        document.getElementById('inputMetodoPagoSeleccionado').value = 'GRATUITO';
        validarYEnviarReserva();
    }
}

function enviarFormularioFinal() {
    const form = document.getElementById('formReservaCompleta');
    const metodo = document.getElementById('inputMetodoPagoSeleccionado').value;

    if (metodo === 'TARJETA') {
        const num = document.getElementById('inputNumeroTarjeta').value.replace(/\s/g, '');
        const titular = document.getElementById('inputTitularTarjeta').value.trim();
        const exp = document.getElementById('inputFechaExpTarjeta').value.trim();
        const cvv = document.getElementById('inputCvvTarjeta').value.trim();

        if (num.length < 13 || !titular || !exp || cvv.length < 3) {
            mostrarResultadoPago('error', 'Datos de tarjeta inválidos',
                'Revisa el número, titular, fecha y CVV de tu tarjeta.');
            return;
        }

        const formData = new FormData(form);
        formData.append('numeroTarjeta', num);
        formData.append('titularTarjeta', titular);
        formData.append('fechaExpTarjeta', exp);
        formData.append('cvvTarjeta', cvv);
        ultimoIntentoPago = formData;
        ejecutarPagoAjax(formData);
        return;
    }

   if (metodo === 'YAPE') {
        const numYape = (document.getElementById('inputNumeroYape').value || '').replace(/\D/g, '');
        const codigoYape = (document.getElementById('inputCodigoYape').value || '').replace(/\D/g, '');

        if (numYape.length !== 9) {
            mostrarResultadoPago('error', 'Número de Yape inválido',
                'Ingresa un número de 9 dígitos que empiece con 9.');
            return;
        }
        if (codigoYape.length !== 6) {
            mostrarResultadoPago('error', 'Código inválido',
                'El código de aprobación debe tener 6 dígitos.');
            return;
        }

        const formData = new FormData(form);
        formData.append('numeroYape', numYape);
        formData.append('codigoYape', codigoYape);
        ultimoIntentoPago = formData;
        ejecutarPagoAjax(formData);
        return;
    }

    if (metodo === 'PLIN') {
        const numPlin = (document.getElementById('inputNumeroPlin').value || '').replace(/\D/g, '');
        const codigoPlin = (document.getElementById('inputCodigoPlin').value || '').trim().toUpperCase();

        if (numPlin.length !== 9) {
            mostrarResultadoPago('error', 'Número de Plin inválido',
                'Ingresa un número de 9 dígitos que empiece con 9.');
            return;
        }
        if (!codigoPlin || codigoPlin.length < 8) {
            mostrarResultadoPago('error', 'Código de operación inválido',
                'Copia el código que aparece debajo del QR (formato PLxxxxxx).');
            return;
        }

        const formData = new FormData(form);
        formData.append('numeroPlin', numPlin);
        formData.append('codigoPlin', codigoPlin);
        ultimoIntentoPago = formData;
        ejecutarPagoAjax(formData);
        return;
    }

    // GRATUITO
    const formData = new FormData(form);
    ultimoIntentoPago = formData;
    ejecutarPagoAjax(formData);
}
function ejecutarPagoAjax(formData) {
    const modalResultado = document.getElementById('modalResultadoPago');
    modalResultado.style.display = 'flex';

    document.getElementById('iconoResultadoPago').textContent = '⏳';
    document.getElementById('tituloResultadoPago').textContent = 'Procesando pago...';
    document.getElementById('tituloResultadoPago').style.color = '#0f172a';
    document.getElementById('mensajeResultadoPago').textContent = 'Por favor espera un momento.';
    document.getElementById('accionesResultadoPago').style.display = 'none';
    document.getElementById('spinnerResultadoPago').style.display = 'block';

    const btnPagar = document.querySelector('.btn-confirmar-pago-final');
    if (btnPagar) { btnPagar.disabled = true; btnPagar.textContent = 'Procesando...'; }

    fetch('/alumno/reservas/confirmar-pago', {
        method: 'POST',
        body: formData,
        headers: {
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'application/json'
        }
    })
        .then(async (response) => {
            const text = await response.text();
            try {
                return JSON.parse(text);
            } catch (e) {
                return { success: false, mensaje: 'Ocurrió un error inesperado. Intenta nuevamente.' };
            }
        })
        .then((data) => {
            if (btnPagar) { btnPagar.disabled = false; btnPagar.textContent = 'Confirmar y Pagar'; }

            if (data.success) {
                mostrarResultadoPago('exito', '¡Pago exitoso! 🎉',
                    data.mensaje || 'Tu reserva ha sido confirmada.');
                setTimeout(() => {
                    cerrarResultadoPago();
                    cerrarModalPago();
                    cerrarWizard();
                    location.reload();
                }, 2000);
            } else {
                mostrarResultadoPago('error', 'Pago rechazado ❌',
                    data.mensaje || 'Tu tarjeta fue rechazada. Verifica los datos o intenta con otro método de pago.');
            }
        })
        .catch(() => {
            if (btnPagar) { btnPagar.disabled = false; btnPagar.textContent = 'Confirmar y Pagar'; }
            mostrarResultadoPago('error', 'Error de conexión',
                'No se pudo procesar el pago. Revisa tu conexión e intenta nuevamente.');
        });
}

// ============================================================================
// 9. SUB-MODAL RESULTADO PAGO
// ============================================================================
function mostrarResultadoPago(tipo, titulo, mensaje) {
    document.getElementById('spinnerResultadoPago').style.display = 'none';
    document.getElementById('accionesResultadoPago').style.display = 'flex';

    const icono = document.getElementById('iconoResultadoPago');
    const tituloEl = document.getElementById('tituloResultadoPago');
    const mensajeEl = document.getElementById('mensajeResultadoPago');
    const btnReintentar = document.getElementById('btnReintentarPago');

    if (tipo === 'exito') {
        icono.textContent = '✅';
        tituloEl.style.color = '#10b981';
        btnReintentar.style.display = 'none';
    } else {
        icono.textContent = '❌';
        tituloEl.style.color = '#ef4444';
        btnReintentar.style.display = 'block';
    }

    tituloEl.textContent = titulo;
    mensajeEl.textContent = mensaje;
}

function reintentarPago() {
    if (ultimoIntentoPago) {
        ejecutarPagoAjax(ultimoIntentoPago);
    }
}

function cerrarResultadoPago() {
    document.getElementById('modalResultadoPago').style.display = 'none';
}

function cerrarModalPago() {
    document.getElementById('modalPagoReserva').style.display = 'none';
    document.getElementById('modalWizard').style.display = 'flex';
}

// ============================================================================
// 10. SELECCIÓN DE MÉTODO DE PAGO
// ============================================================================
let codigoPlinActual = null;

function generarQrPlin(codigo) {
    const cont = document.getElementById('qrcodePlin');
    if (!cont) return;
    cont.innerHTML = ''; // limpiar el QR anterior
    if (typeof QRCode === 'undefined') {
        console.warn('QRCode.js no está cargado');
        return;
    }
    new QRCode(cont, {
        text: codigo,
        width: 180,
        height: 180,
        colorDark: "#0f172a",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.H
    });
    document.getElementById('codigoQrPlin').innerText = codigo;
}

function generarCodigoPlinAleatorio() {
    const num = Math.floor(100000 + Math.random() * 900000);
    return 'PL' + num;
}

function seleccionarMetodo(opc, met) {
    document.querySelectorAll('.opcion-metodo-pago').forEach(o => o.classList.remove('seleccionado'));
    opc.classList.add('seleccionado');
    document.getElementById('inputMetodoPagoSeleccionado').value = met;

    const seccionBilleteras = document.getElementById('seccionBilleterasPago');
    const subYape = document.getElementById('subSeccionYape');
    const subPlin = document.getElementById('subSeccionPlin');
    const seccionTarjeta = document.getElementById('seccionTarjetaPago');
    const icono = document.getElementById('iconoBilletera');
    const subtitulo = document.getElementById('subtituloBilletera');

    if (met === 'TARJETA') {
        if (seccionBilleteras) seccionBilleteras.style.display = 'none';
        if (seccionTarjeta) seccionTarjeta.style.display = 'block';
        return;
    }

    if (seccionTarjeta) seccionTarjeta.style.display = 'none';
    if (seccionBilleteras) seccionBilleteras.style.display = 'block';

    if (met === 'YAPE') {
        icono.textContent = '📱';
        subtitulo.textContent = 'Ingresa tu número de Yape y el código de 6 dígitos';
        if (subYape) subYape.style.display = 'block';
        if (subPlin) subPlin.style.display = 'none';
    } else if (met === 'PLIN') {
        icono.textContent = '💚';
        subtitulo.textContent = 'Escanea el QR o copia el código de operación';
        if (subYape) subYape.style.display = 'none';
        if (subPlin) subPlin.style.display = 'block';

        // Generar un código nuevo cada vez que se elige Plin
        codigoPlinActual = generarCodigoPlinAleatorio();
        generarQrPlin(codigoPlinActual);
    }
}

// ============================================================================
// 11. MODAL RESUMEN DE RESERVA
// ============================================================================
function abrirModalResumen(tarjeta) {
    const idReserva = tarjeta.getAttribute('data-id-reserva');
    if (!idReserva) return;

    const formCancelar = document.getElementById('formCancelarReserva');
    if (formCancelar) {
        formCancelar.action = `/alumno/reservas/${idReserva}/cancelar`;
    }

    document.getElementById('resumenId').innerText = idReserva;
    document.getElementById('modalResumenReserva').style.display = 'flex';

    fetch(`/alumno/api/recibo/${idReserva}`)
        .then(res => res.json())
        .then(data => {
            if (!data.encontrado) {
                alert('No se encontró la reserva.');
                cerrarModalResumen();
                return;
            }

            document.getElementById('resumenLab').innerText = data.laboratorio;
            document.getElementById('resumenCubiculo').innerText = data.cubiculo;
            document.getElementById('resumenFecha').innerText = data.fecha;
            document.getElementById('resumenHorario').innerText =
                data.horaInicio.substring(0, 5) + ' – ' + data.horaFin.substring(0, 5);

            const estadoBadge = document.getElementById('resumenEstado');
            estadoBadge.innerText = data.estado;
            estadoBadge.className = 'badge-estado-recibo';
            if (data.estado === 'CONFIRMADA') estadoBadge.classList.add('confirmada');
            else if (data.estado === 'EN_USO') estadoBadge.classList.add('enuso');
            else if (data.estado === 'DEVUELTA') estadoBadge.classList.add('devuelta');

            const lblEncargado = document.getElementById('resumenEncargado');
            if (lblEncargado) {
                lblEncargado.innerText = data.nombreEncargado || 'Por asignar';
            }

            const contInteg = document.getElementById('resumenIntegrantes');
            contInteg.innerHTML = '';
            data.integrantes.forEach(i => {
                contInteg.insertAdjacentHTML('beforeend', `
                    <div class="item-integrante-recibo">
                        <div class="avatar-mini-recibo">${i.nombre.substring(0, 2).toUpperCase()}</div>
                        <div>
                            <strong>${i.nombre}</strong>
                            <span>${i.codigo}</span>
                        </div>
                    </div>
                `);
            });

            const contItems = document.getElementById('resumenItems');
            const seccionItems = document.getElementById('seccionInsumosResumen');
            contItems.innerHTML = '';
            if (!data.items || data.items.length === 0) {
                seccionItems.style.display = 'none';
            } else {
                seccionItems.style.display = 'block';
                data.items.forEach(item => {
                    contItems.insertAdjacentHTML('beforeend', `
                        <div class="item-recibo">
                            <span class="nombre-item-recibo">${item.nombre}</span>
                            <span class="cantidad-item-recibo">x${item.cantidad}</span>
                        </div>
                    `);
                });
            }
            actualizarEstadoReservaEnVivo(idReserva, data.estado);
        })
        .catch(() => {
            alert('No se pudo cargar el resumen de la reserva.');
            cerrarModalResumen();
        });
}

function cerrarModalResumen() {
    document.getElementById('modalResumenReserva').style.display = 'none';
}

// ============================================================================
// 12. ACTUALIZACIÓN EN VIVO DEL ESTADO DE LA RESERVA
// ============================================================================
function actualizarEstadoReservaEnVivo(idReserva, nuevoEstado) {
    const tarjeta = document.querySelector(`.tarjeta-reserva-activa[data-id-reserva="${idReserva}"]`);
    if (!tarjeta) return;

    const badge = tarjeta.querySelector('.badge-estado-historial');
    if (badge) {
        badge.className = 'badge-estado-historial';
        if (nuevoEstado === 'EN_USO') {
            badge.classList.add('enuso');
            badge.innerText = 'En Uso';
        } else if (nuevoEstado === 'DEVUELTA') {
            badge.classList.add('devuelta');
            badge.innerText = 'Devuelta';
        } else if (nuevoEstado === 'INSPECCIONADA') {
            badge.classList.add('inspeccionada');
            badge.innerText = 'Inspeccionada';
        } else if (nuevoEstado === 'CONFIRMADA') {
            badge.classList.add('confirmada');
            badge.innerText = 'Confirmada';
        } else if (nuevoEstado === 'CANCELADA') {
            badge.classList.add('cancelada');
            badge.innerText = 'Cancelada';
        }
    }

    if (nuevoEstado === 'EN_USO') {
        const contador = document.querySelector('.alert-info');
        if (contador) {
            const text = contador.innerText;
            const match = text.match(/Hoy llevas (\d+) de 2/);
            if (match) {
                const reservasHoy = parseInt(match[1]) + 1;
                const restantes = Math.max(0, 2 - reservasHoy);
                contador.innerHTML = `Hoy llevas <strong>${reservasHoy}</strong> de 2 reservas permitidas. Te quedan <strong>${restantes}</strong>.`;

                if (reservasHoy >= 2) {
                    const btnNuevaReserva = document.querySelector('.btn-nueva-reserva');
                    if (btnNuevaReserva) btnNuevaReserva.style.display = 'none';
                }
            }
        }
    }
}

// ============================================================================
// 13. VIGILANTE AUTOMÁTICO (cada 30s)
// ============================================================================
setInterval(() => {
    const tarjetas = document.querySelectorAll('.tarjeta-reserva-activa');
    tarjetas.forEach(tarjeta => {
        const idReserva = tarjeta.getAttribute('data-id-reserva');
        if (!idReserva) return;

        fetch(`/alumno/api/recibo/${idReserva}`)
            .then(res => res.json())
            .then(data => {
                if (data.encontrado) {
                    actualizarEstadoReservaEnVivo(idReserva, data.estado);
                }
            })
            .catch(() => { /* silencio */ });
    });
}, 30000);

// ============================================================================
// 14. CANCELAR RESERVA
// ============================================================================
function confirmarCancelacionReserva() {
    if (!confirm('¿Estás seguro de que deseas cancelar esta reserva? Esta acción liberará el espacio.')) {
        return;
    }
    const form = document.getElementById('formCancelarReserva');
    form.submit();
}