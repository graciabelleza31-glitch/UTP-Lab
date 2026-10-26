document.addEventListener("DOMContentLoaded", function () {
    if (typeof lucide !== 'undefined') lucide.createIcons();
});

function toggleNotificaciones(event) {
    event.stopPropagation();
    const burbuja = document.getElementById('burbujaNotificaciones');
    if (!burbuja) return;
    if (burbuja.style.display === 'none' || burbuja.style.display === '') {
        burbuja.style.display = 'block';
        if (typeof lucide !== 'undefined') lucide.createIcons();
    } else {
        burbuja.style.display = 'none';
    }
}

function cerrarNotificaciones() {
    const burbuja = document.getElementById('burbujaNotificaciones');
    if (burbuja) burbuja.style.display = 'none';
}

document.addEventListener('click', function (event) {
    const burbuja = document.getElementById('burbujaNotificaciones');
    const boton = document.querySelector('.boton-campana-amarilla');
    if (burbuja && boton && !boton.contains(event.target) && !burbuja.contains(event.target)) {
        burbuja.style.display = 'none';
    }
});

function cargarNotificaciones() {
    const contenedor = document.getElementById('listaNotificaciones');
    if (!contenedor) return;

    fetch('/alumno/api/notificaciones')
        .then(res => res.json())
        .then(data => {
            contenedor.innerHTML = '';
            if (data.length === 0) {
                contenedor.innerHTML = `
                    <div class="sin-notificaciones">
                        <i data-lucide="check-circle"></i>
                        <p>No tienes notificaciones pendientes</p>
                    </div>
                `;
            } else {
                data.forEach(notif => {
                    contenedor.insertAdjacentHTML('beforeend', `
                        <a href="${notif.url}" class="item-notificacion">
                            <div class="icono-notif icono-notif-rojo">
                                <i data-lucide="alert-triangle"></i>
                            </div>
                            <div class="info-notif">
                                <strong>${notif.titulo}</strong>
                                <span>${notif.mensaje}</span>
                            </div>
                        </a>
                    `);
                });
            }
            if (typeof lucide !== 'undefined') lucide.createIcons();
        })
        .catch(err => console.error("Error al cargar notificaciones", err));
}

// Sobrescribimos la función original para que cargue las notificaciones antes de abrir
const toggleOriginal = toggleNotificaciones;
window.toggleNotificaciones = function(event) {
    toggleOriginal(event);
    cargarNotificaciones();
};