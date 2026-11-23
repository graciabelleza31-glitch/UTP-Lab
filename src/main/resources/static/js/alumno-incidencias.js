// ============================================================================
// ALUMNO - INCIDENCIAS · PAGO DINÁMICO CON YAPE / PLIN / TARJETA
// ============================================================================
let idSancionActual = null;
let montoActualTexto = 'S/ 0.00';
let metodoPagoActual = 'YAPE';

document.addEventListener("DOMContentLoaded", function () {
    if (typeof lucide !== 'undefined') lucide.createIcons();
});

// ============================================================================
// ABRIR / CERRAR MODAL DE PAGO
// ============================================================================
function abrirModalPago(btn) {
    idSancionActual = btn.getAttribute('data-id');
    montoActualTexto = btn.getAttribute('data-monto') || 'S/ 0.00';

    const modalTitulo = document.getElementById('modalTituloInc');
    if (modalTitulo) {
        const item = btn.getAttribute('data-item') || 'Material de Lab';
        modalTitulo.innerText = `Pago de Incidencia · ${item}`;
    }

    document.getElementById('modalMontoPagar').innerText = montoActualTexto;
    const btnPagar = document.getElementById('btnConfirmarPago');
    if (btnPagar) {
        btnPagar.disabled = false;
        btnPagar.innerText = 'Confirmar pago · ' + montoActualTexto;
    }

    // Limpiar alertas, campos y avisos dinámicos
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) alerta.style.display = 'none';

    const avisoYape = document.getElementById('avisoCodigoYapeInc');
    if (avisoYape) avisoYape.style.display = 'none';

    const contQrPlin = document.getElementById('contenedorQrPlinInc');
    if (contQrPlin) contQrPlin.style.display = 'none';

    const bloqueCodPlin = document.getElementById('bloqueCodigoPlinInc');
    if (bloqueCodPlin) bloqueCodPlin.style.display = 'none';

    ['inputNumeroTarjeta', 'inputTitularTarjeta', 'inputFechaExpTarjeta', 'inputCvvTarjeta',
     'inputNumeroYapeInc', 'inputCodigoYapeInc',
     'inputNumeroPlinInc', 'inputCodigoPlinInc'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });

    document.getElementById('pasoPagoSeleccion').style.display = 'block';
    document.getElementById('pasoPagoExitoso').style.display = 'none';
    document.getElementById('modalPagoIncidencia').style.display = 'flex';

    // Seleccionar Yape por defecto
    const primerMetodo = document.querySelector('#modalPagoIncidencia .grid-metodos-pago .opcion-metodo');
    if (primerMetodo) seleccionarMetodoInc(primerMetodo, 'YAPE');

    if (typeof lucide !== 'undefined') lucide.createIcons();
}

function cerrarModalPago() {
    const m = document.getElementById('modalPagoIncidencia');
    if (m) m.style.display = 'none';
}

// ============================================================================
// SELECCIÓN DE MÉTODO (YAPE / PLIN / TARJETA)
// ============================================================================
function seleccionarMetodoInc(el, metodo) {
    metodoPagoActual = metodo;
    document.querySelectorAll('#modalPagoIncidencia .grid-metodos-pago .opcion-metodo')
        .forEach(m => m.classList.remove('seleccionado'));
    if (el) el.classList.add('seleccionado');

    const secYape = document.getElementById('seccionYapeIncidencia');
    const secPlin = document.getElementById('seccionPlinIncidencia');
    const secTarjeta = document.getElementById('seccionTarjeta');
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) alerta.style.display = 'none';

    if (secYape) secYape.style.display = (metodo === 'YAPE') ? 'block' : 'none';
    if (secPlin) secPlin.style.display = (metodo === 'PLIN') ? 'block' : 'none';
    if (secTarjeta) secTarjeta.style.display = (metodo === 'TARJETA') ? 'block' : 'none';
}

// ============================================================================
// SOLICITAR CÓDIGO DINÁMICO DE YAPE (BACKEND)
// ============================================================================
async function solicitarCodigoYapeInc() {
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) alerta.style.display = 'none';

    const numYape = (document.getElementById('inputNumeroYapeInc').value || '').replace(/\D/g, '');
    if (numYape.length !== 9) {
        mostrarError('Ingresa un número de celular Yape de 9 dígitos.');
        return;
    }

    try {
        const resp = await fetch('/alumno/api/pagos/yape/generar-codigo', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ numeroYape: numYape })
        });

        if (!resp.ok) {
            mostrarError('Error del servidor (' + resp.status + ') al solicitar el código.');
            return;
        }

        const data = await resp.json();
        if (!data.success) {
            document.getElementById('avisoCodigoYapeInc').style.display = 'none';
            mostrarError(data.mensaje || 'No se pudo generar el código de Yape.');
            return;
        }

        document.getElementById('titularYapeInc').innerText = data.titular;
        document.getElementById('codigoYapeSimuladoInc').innerText = data.codigoAprobacion;
        document.getElementById('avisoCodigoYapeInc').style.display = 'block';
    } catch (e) {
        mostrarError('Error de conexión al solicitar el código de Yape.');
    }
}

// ============================================================================
// GENERAR QR DINÁMICO DE PLIN (BACKEND)
// ============================================================================
async function solicitarQrPlinInc() {
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) alerta.style.display = 'none';

    const numPlin = (document.getElementById('inputNumeroPlinInc').value || '').replace(/\D/g, '');
    if (numPlin.length !== 9) {
        mostrarError('Ingresa un número de celular Plin de 9 dígitos.');
        return;
    }

    try {
        const resp = await fetch('/alumno/api/pagos/plin/generar-qr', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ numeroPlin: numPlin })
        });

        if (!resp.ok) {
            mostrarError('Error del servidor (' + resp.status + ') al generar el QR.');
            return;
        }

        const data = await resp.json();
        if (!data.success) {
            document.getElementById('contenedorQrPlinInc').style.display = 'none';
            document.getElementById('bloqueCodigoPlinInc').style.display = 'none';
            mostrarError(data.mensaje || 'No se pudo generar el QR de Plin.');
            return;
        }

        document.getElementById('titularPlinInc').innerText = `Titular: ${data.titular}`;
        document.getElementById('contenedorQrPlinInc').style.display = 'block';
        document.getElementById('bloqueCodigoPlinInc').style.display = 'block';

        const cont = document.getElementById('qrPlinIncidencia');
        cont.innerHTML = '';
        if (typeof QRCode !== 'undefined') {
            new QRCode(cont, {
                text: `UTP +Lab Plin | Codigo: ${data.codigoOperacion} | Monto: ${montoActualTexto}`,
                width: 135,
                height: 135,
                colorDark: "#0f172a",
                colorLight: "#ffffff",
                correctLevel: QRCode.CorrectLevel.M
            });
        }
    } catch (e) {
        mostrarError('Error de conexión al generar el QR de Plin.');
    }
}

// ============================================================================
// PROCESAR PAGO (AJAX)
// ============================================================================
async function ejecutarPagoBackend() {
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) alerta.style.display = 'none';

    if (!idSancionActual) return;

    const btnPagar = document.getElementById('btnConfirmarPago');
    const formData = new URLSearchParams();
    formData.append('metodoPago', metodoPagoActual);

    if (metodoPagoActual === 'TARJETA') {
        const num = document.getElementById('inputNumeroTarjeta').value.replace(/\s/g, '');
        const titular = document.getElementById('inputTitularTarjeta').value.trim();
        const exp = document.getElementById('inputFechaExpTarjeta').value.trim();
        const cvv = document.getElementById('inputCvvTarjeta').value.trim();

        if (!num || num.length < 16) return mostrarError("Ingresa un número de tarjeta válido (16 dígitos).", btnPagar);
        if (!titular) return mostrarError("Ingresa el nombre del titular.", btnPagar);
        if (!exp || !cvv) return mostrarError("Completa la fecha de expiración y el CVV.", btnPagar);

        formData.append('numeroTarjeta', num);
        formData.append('titularTarjeta', titular);
        formData.append('fechaExpTarjeta', exp);
        formData.append('cvvTarjeta', cvv);
    }

    if (metodoPagoActual === 'YAPE') {
        const numYape = (document.getElementById('inputNumeroYapeInc').value || '').replace(/\D/g, '');
        const codigoYape = (document.getElementById('inputCodigoYapeInc').value || '').replace(/\D/g, '');

        if (numYape.length !== 9) return mostrarError("El número de Yape debe tener 9 dígitos.", btnPagar);
        if (codigoYape.length !== 6) return mostrarError("El código de aprobación debe tener 6 dígitos.", btnPagar);

        formData.append('numeroYape', numYape);
        formData.append('codigoYape', codigoYape);
    }

    if (metodoPagoActual === 'PLIN') {
        const numPlin = (document.getElementById('inputNumeroPlinInc').value || '').replace(/\D/g, '');
        const codigoPlin = (document.getElementById('inputCodigoPlinInc').value || '').trim().toUpperCase();

        if (numPlin.length !== 9) return mostrarError("El número de Plin debe tener 9 dígitos.", btnPagar);
        if (!codigoPlin || codigoPlin.length < 8) return mostrarError("Ingresa el código de operación que aparece al escanear el QR.", btnPagar);

        formData.append('numeroPlin', numPlin);
        formData.append('codigoPlin', codigoPlin);
    }

    btnPagar.disabled = true;
    btnPagar.innerText = "Procesando pago...";

    try {
        const resp = await fetch('/alumno/incidencias/pagar/' + encodeURIComponent(idSancionActual), {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        });

        const data = await resp.json();

        if (!resp.ok) {
            mostrarError(data.error || "Ocurrió un error al procesar el pago.", btnPagar);
            return;
        }

        document.getElementById('subtextoExitoMonto').innerText = montoActualTexto + ' procesados correctamente';
        document.getElementById('textoTxnCode').innerText = data.transaccion || 'TXN-000';
        document.getElementById('pasoPagoSeleccion').style.display = 'none';
        document.getElementById('pasoPagoExitoso').style.display = 'flex';

        if (typeof lucide !== 'undefined') lucide.createIcons();

    } catch (err) {
        console.error(err);
        mostrarError("Error de conexión con el servidor.", btnPagar);
    }
}

function mostrarError(mensaje, btnPagar) {
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) {
        alerta.innerText = mensaje;
        alerta.style.display = 'block';
    }
    if (btnPagar) {
        btnPagar.disabled = false;
        btnPagar.innerText = 'Confirmar pago · ' + montoActualTexto;
    }
}

function finalizarPagoYRecargar() {
    cerrarModalPago();
    window.location.reload();
}