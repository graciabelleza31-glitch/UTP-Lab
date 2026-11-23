let catFiltro = 'TODOS';

function filtrarCategoria(btn) {
  document.querySelectorAll('.btn-filtro-cat').forEach(b => b.classList.remove('activo'));
  btn.classList.add('activo');
  catFiltro = (btn.getAttribute('data-cat') || 'TODOS').trim().toLowerCase();
  filtrarInventario();
}

function filtrarInventario() {
  const busq = document.getElementById('inputBuscarInventario').value.toLowerCase().trim();
  const filas = document.querySelectorAll('.fila-producto-inv');
  let visibles = 0;

  filas.forEach(f => {
    const nombre = (f.getAttribute('data-nombre') || '').toLowerCase();
    const codigo = (f.getAttribute('data-codigo') || '').toLowerCase();
    const cat = (f.getAttribute('data-categoria') || '').toLowerCase();

    const coincideTxt = nombre.includes(busq) || codigo.includes(busq);
    const coincideCat = (catFiltro === 'todos') || (cat === catFiltro);

    if (coincideTxt && coincideCat) {
      f.style.display = '';
      visibles++;
    } else {
      f.style.display = 'none';
    }
  });

  document.getElementById('badgeTotalItems').innerText = `${visibles} registrados`;
}

function previsualizarImagenInsumo(input) {
  const preview = document.getElementById('imgPreviewInsumo');
  const labelNombre = document.getElementById('nombreArchivoSeleccionado');

  if (input.files && input.files[0]) {
    const archivo = input.files[0];
    if (labelNombre) labelNombre.innerText = archivo.name;

    const reader = new FileReader();
    reader.onload = function (e) {
      if (preview) preview.src = e.target.result;
    };
    reader.readAsDataURL(archivo);
  } else {
    if (preview) preview.src = '/img/insumos/default.jpg';
    if (labelNombre) labelNombre.innerText = 'Sin archivo';
  }
}

function cerrarModalNuevoProducto() {
  document.getElementById('modalNuevoProducto').style.display = 'none';
  // Resetear vista previa al cerrar
  const inputImg = document.getElementById('inputImagenInsumo');
  if (inputImg) inputImg.value = '';
  const preview = document.getElementById('imgPreviewInsumo');
  if (preview) preview.src = '/img/insumos/default.jpg';
  const labelNombre = document.getElementById('nombreArchivoSeleccionado');
  if (labelNombre) labelNombre.innerText = 'Sin archivo';
}

function ejecutarAjusteStock(btn) {
  const idProducto = btn.getAttribute('data-id');
  const cantidad = parseInt(btn.getAttribute('data-cant'), 10);

  const form = new FormData();
  form.append('cantidad', cantidad);

  fetch(`/admin/inventario/actualizar-stock/${idProducto}`, {
    method: 'POST',
    body: form
  })
  .then(res => {
    if (!res.ok) throw new Error("Error en servidor");
    return res.json();
  })
  .then(data => {
    if (data.nuevoStock !== undefined) {
      const spanStock = document.getElementById(`stock-val-${idProducto}`);
      if (spanStock) spanStock.innerText = data.nuevoStock;
      
      const fila = btn.closest('tr');
      if (fila) {
        const tdEstado = fila.children[6];
        const stock = data.nuevoStock;
        if (stock > 10) {
          tdEstado.innerHTML = '<span class="badge-estado-stock disp">✓ Disp.</span>';
        } else if (stock > 0) {
          tdEstado.innerHTML = '<span class="badge-estado-stock bajo">⚠ Bajo</span>';
        } else {
          tdEstado.innerHTML = '<span class="badge-estado-stock agotado">✕ Agotado</span>';
        }
      }
    }
  })
  .catch(() => alert('Error al actualizar el stock.'));
}

function abrirModalNuevoProducto() {
  document.getElementById('modalNuevoProducto').style.display = 'flex';
}

function cerrarModalNuevoProducto() {
  document.getElementById('modalNuevoProducto').style.display = 'none';
}