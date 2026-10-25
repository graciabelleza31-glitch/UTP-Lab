document.addEventListener("DOMContentLoaded", function () {
  if (typeof lucide !== 'undefined') lucide.createIcons();
});

let categoriaFiltroActual = 'TODOS';

function seleccionarCategoriaBtn(btn) {
  document.querySelectorAll('.btn-filtro-cat').forEach(b => b.classList.remove('activo'));
  btn.classList.add('activo');
  categoriaFiltroActual = (btn.getAttribute('data-cat') || 'TODOS').trim().toLowerCase();
  filtrarCatalogo();
}

function filtrarCatalogo() {
  const busqueda = document.getElementById('inputBuscarCatalogo').value.toLowerCase().trim();
  const tarjetas = document.querySelectorAll('.tarjeta-insumo-catalogo');
  let visibles = 0;

  tarjetas.forEach(card => {
    const nombre = (card.getAttribute('data-nombre') || '').toLowerCase();
    const categoria = (card.getAttribute('data-categoria') || '').toLowerCase();

    const coincideTexto = nombre.includes(busqueda);
    const coincideCat = (categoriaFiltroActual === 'todos') || (categoria === categoriaFiltroActual);

    if (coincideTexto && coincideCat) {
      card.style.display = 'flex';
      visibles++;
    } else {
      card.style.display = 'none';
    }
  });

  document.getElementById('badgeContadorItems').innerText = `${visibles} items`;
}