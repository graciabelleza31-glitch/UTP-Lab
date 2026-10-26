document.addEventListener("DOMContentLoaded", function () {
    if (typeof lucide !== 'undefined') lucide.createIcons();
});

function toggleNotificaciones(event) {
    event.stopPropagation();
    const burbuja = document.getElementById('burbujaNotificaciones');
    if (burbuja.style.display === 'none' || burbuja.style.display === '') {
        burbuja.style.display = 'block';
        if (typeof lucide !== 'undefined') lucide.createIcons();
    } else {
        burbuja.style.display = 'none';
    }
}

function cerrarNotificaciones() {
    document.getElementById('burbujaNotificaciones').style.display = 'none';
}

document.addEventListener('click', function (event) {
    const burbuja = document.getElementById('burbujaNotificaciones');
    const boton = document.querySelector('.boton-campana-amarilla');
    if (burbuja && boton && !boton.contains(event.target) && !burbuja.contains(event.target)) {
        burbuja.style.display = 'none';
    }
});