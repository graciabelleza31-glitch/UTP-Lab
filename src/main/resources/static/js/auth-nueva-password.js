const passwordInput = document.getElementById('password');
const passwordConfirmInput = document.getElementById('passwordConfirm');
const btnEnviar = document.getElementById('btnEnviar');
const mensajeCoincidencia = document.getElementById('mensajeCoincidencia');
const textoAyuda = document.getElementById('textoAyuda');

const reqLongitud = document.getElementById('req-longitud');
const reqMayuscula = document.getElementById('req-mayuscula');
const reqNumero = document.getElementById('req-numero');
const reqSimbolo = document.getElementById('req-simbolo');

function actualizarRequisito(elemento, cumplido) {
    const icono = elemento.querySelector('.icono');
    if (cumplido) {
        elemento.classList.add('cumplido');
        icono.textContent = '✔️';
    } else {
        elemento.classList.remove('cumplido');
        icono.textContent = '✖️';
    }
}

function validarFormulario() {
    const val = passwordInput.value;
    const confirmVal = passwordConfirmInput.value;

    const tieneLongitud = val.length >= 6;
    const tieneMayuscula = /[A-Z]/.test(val);
    const tieneNumero = /[0-9]/.test(val);
    const tieneSimbolo = /[@$!%*?&._\-#]/.test(val);

    actualizarRequisito(reqLongitud, tieneLongitud);
    actualizarRequisito(reqMayuscula, tieneMayuscula);
    actualizarRequisito(reqNumero, tieneNumero);
    actualizarRequisito(reqSimbolo, tieneSimbolo);

    const passwordValida = tieneLongitud && tieneMayuscula && tieneNumero && tieneSimbolo;

    let coinciden = false;
    if (confirmVal.length > 0) {
        textoAyuda.style.display = 'none';
        mensajeCoincidencia.style.display = 'block';
        if (val === confirmVal) {
            mensajeCoincidencia.style.color = '#16a34a';
            mensajeCoincidencia.textContent = '✔️ Las contraseñas coinciden';
            coinciden = true;
        } else {
            mensajeCoincidencia.style.color = '#dc2626';
            mensajeCoincidencia.textContent = '✖️ Las contraseñas no coinciden';
            coinciden = false;
        }
    } else {
        textoAyuda.style.display = 'block';
        mensajeCoincidencia.style.display = 'none';
    }

    btnEnviar.disabled = !(passwordValida && coinciden);
}

passwordInput.addEventListener('input', validarFormulario);
passwordConfirmInput.addEventListener('input', validarFormulario);