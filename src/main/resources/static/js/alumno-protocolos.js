document.addEventListener("DOMContentLoaded", function () {
  if (typeof lucide !== 'undefined') lucide.createIcons();
});

function toggleAcordeon(idItem) {
  const item = document.getElementById(idItem);
  const estaAbierto = item.classList.contains('abierto');

  // Cerrar todos los acordeones
  document.querySelectorAll('.acordeon-item').forEach(acc => {
    acc.classList.remove('abierto');
    const flecha = acc.querySelector('.flecha-indicador');
    if (flecha) flecha.innerText = '▼';
  });

  // Si no estaba abierto, lo abrimos
  if (!estaAbierto) {
    item.classList.add('abierto');
    const flecha = item.querySelector('.flecha-indicador');
    if (flecha) flecha.innerText = '▲';
  }
}

function descargarTerminos() {
    window.open('/docs/terminos.pdf', '_blank');
}