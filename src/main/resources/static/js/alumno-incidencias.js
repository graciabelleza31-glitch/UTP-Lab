// ============================================================================
// ALUMNO - INCIDENCIAS · PAGO CON YAPE / PLIN / TARJETA
// ============================================================================
let idSancionActual = null;
let montoActualTexto = 'S/ 0.00';
let metodoPagoActual = 'YAPE';
let codigoPlinIncActual = null;

// Inicializar íconos
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
    document.getElementById('btnConfirmarPago').innerText = 'Confirmar pago · ' + montoActualTexto;

    // Limpiar alertas y campos
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) alerta.style.display = 'none';

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
    const primerMetodo = document.querySelector('.grid-metodos-pago .opcion-metodo');
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
    document.querySelectorAll('.grid-metodos-pago .opcion-metodo').forEach(m => m.classList.remove('seleccionado'));
    if (el) el.classList.add('seleccionado');

    const secYape = document.getElementById('seccionYapeIncidencia');
    const secPlin = document.getElementById('seccionPlinIncidencia');
    const secTarjeta = document.getElementById('seccionTarjeta');
    const alerta = document.getElementById('alertaErrorIncidencia');
    if (alerta) alerta.style.display = 'none';

    if (secYape) secYape.style.display = (metodo === 'YAPE') ? 'block' : 'none';
    if (secPlin) secPlin.style.display = (metodo === 'PLIN') ? 'block' : 'none';
    if (secTarjeta) secTarjeta.style.display = (metodo === 'TARJETA') ? 'block' : 'none';

    if (metodo === 'PLIN') {
        generarCodigoPlinInc();
    }
}

// Alias para compatibilidad con el HTML si llama a seleccionarMetodo()
function seleccionarMetodo(el, metodo) {
    seleccionarMetodoInc(el, metodo);
}

// ============================================================================
// GENERAR QR DE PLIN
// ============================================================================
function generarCodigoPlinInc() {
    // ⚠️ Demo: código fijo para que apruebe con 987654321 en la BD
    codigoPlinIncActual = 'PL123456';

    const txt = document.getElementById('codigoQrPlinIncidencia');
    if (txt) txt.innerText = codigoPlinIncActual;

    const cont = document.getElementById('qrPlinIncidencia');
    if (!cont) return;
    cont.innerHTML = '';

    if (typeof QRCode === 'undefined') {
        console.warn('QRCode.js no está cargado. Agrega el <script> en el <head>.');
        return;
    }

    new QRCode(cont, {
        text: codigoPlinIncActual,
        width: 150,
        height: 150,
        colorDark: "#0f172a",
        colorLight: "#ffffff",
        correctLevel: QRCode.CorrectLevel.H
    });
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

    // ---------- VALIDACIONES LOCALES ----------
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
        if (!codigoPlin || codigoPlin.length < 8) return mostrarError("Copia el código de operación que aparece debajo del QR.", btnPagar);

        formData.append('numeroPlin', numPlin);
        formData.append('codigoPlin', codigoPlin);
    }

    // ---------- ENVÍO AJAX ----------
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

        // ✅ Pago exitoso
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

// ============================================================================
// FINALIZAR Y RECARGAR
// ============================================================================
function finalizarPagoYRecargar() {
    cerrarModalPago();
    window.location.reload();
}