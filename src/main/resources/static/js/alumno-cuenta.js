function abrirRecibo(fila) {
    const idReserva = fila.getAttribute('data-id-reserva');
    if (!idReserva) return;
    
    document.getElementById('reciboId').innerText = idReserva;
    document.getElementById('modalRecibo').style.display = 'flex';
    
    fetch(`/alumno/api/recibo/${idReserva}`)
        .then(res => res.json())
        .then(data => {
            if (!data.encontrado) {
                alert('No se encontró el recibo.');
                cerrarRecibo();
                return;
            }
            
            document.getElementById('reciboLab').innerText = data.laboratorio;
            document.getElementById('reciboCubiculo').innerText = data.cubiculo;
            document.getElementById('reciboFecha').innerText = data.fecha;
            document.getElementById('reciboHorario').innerText = data.horaInicio.substring(0, 5) + ' – ' + data.horaFin.substring(0, 5);
            
            const estadoBadge = document.getElementById('reciboEstado');
            estadoBadge.innerText = data.estado;
            estadoBadge.className = 'badge-estado-recibo';
            if (data.estado === 'CONFIRMADA') estadoBadge.classList.add('confirmada');
            else if (data.estado === 'EN_USO') estadoBadge.classList.add('enuso');
            else if (data.estado === 'DEVUELTA') estadoBadge.classList.add('devuelta');
            else estadoBadge.classList.add('cancelada');
            
            const contInteg = document.getElementById('reciboIntegrantes');
            contInteg.innerHTML = '';
            data.integrantes.forEach(i => {
                contInteg.insertAdjacentHTML('beforeend', `
                    <div class="item-integrante-recibo">
                        <div class="avatar-mini-recibo">${i.nombre.substring(0,2).toUpperCase()}</div>
                        <div>
                            <strong>${i.nombre}</strong>
                            <span>${i.codigo}</span>
                        </div>
                    </div>
                `);
            });
            
            const contItems = document.getElementById('reciboItems');
            const seccionItems = document.getElementById('seccionInsumosRecibo');
            contItems.innerHTML = '';
            if (data.items.length === 0) {
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
            
            document.getElementById('reciboMetodoPago').innerText = data.metodoPago.replace('PAGO_', '');
            document.getElementById('reciboMontoTotal').innerText = 'S/ ' + parseFloat(data.montoTotal).toFixed(2);
        })
        .catch(() => {
            alert('Error al cargar el recibo.');
            cerrarRecibo();
        });
}

function cerrarRecibo() {
    document.getElementById('modalRecibo').style.display = 'none';
}

function abrirModalIncidencia(fila) {
    const idSancion = fila.getAttribute('data-id-sancion');
    if (!idSancion) return;

    document.getElementById('modalDetalleIncidencia').style.display = 'flex';
    document.getElementById('incId').innerText = idSancion;

    fetch(`/alumno/api/incidencia/${idSancion}`)
        .then(res => res.json())
        .then(data => {
            if (!data.encontrado) {
                alert('No se encontró la incidencia.');
                cerrarModalIncidencia();
                return;
            }

            document.getElementById('incMotivo').innerText = data.motivo;
            document.getElementById('incReserva').innerText = data.idReserva;
            document.getElementById('incFecha').innerText = data.fechaReserva;
            document.getElementById('incLab').innerText = data.laboratorio;
            document.getElementById('incCubiculo').innerText = data.cubiculo;
            document.getElementById('incHorario').innerText = data.horario;
            document.getElementById('incMonto').innerText = 'S/ ' + parseFloat(data.monto).toFixed(2);

            const badge = document.getElementById('incEstado');
            badge.innerText = data.estado;
            badge.className = 'badge-estado-recibo';
            if (data.estado === 'PAGADA' || data.estado === 'RESUELTA') badge.classList.add('confirmada');
            else if (data.estado === 'NOTIFICADA') badge.classList.add('enuso');
            else badge.classList.add('cancelada');

            if (data.estado === 'PAGADA' || data.estado === 'RESUELTA') {
                document.getElementById('filaMetodoPago').style.display = 'flex';
                document.getElementById('filaFechaPago').style.display = 'flex';
                document.getElementById('incMetodo').innerText = data.metodoPago ? data.metodoPago.replace('PAGO_', '') : 'YAPE';
// Después (formateado):
if (data.fechaPago) {
    const fecha = new Date(data.fechaPago);
    document.getElementById('incFechaPago').innerText = fecha.toLocaleDateString('es-PE', { 
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' 
    });
} else {
    document.getElementById('incFechaPago').innerText = '—';
}
            } else {
                document.getElementById('filaMetodoPago').style.display = 'none';
                document.getElementById('filaFechaPago').style.display = 'none';
            }

            const contInteg = document.getElementById('incIntegrantes');
            contInteg.innerHTML = '';
            if (data.integrantes) {
                data.integrantes.forEach(i => {
                    contInteg.insertAdjacentHTML('beforeend', `
                        <div class="item-integrante-recibo">
                            <div class="avatar-mini-recibo">${i.nombre.substring(0,2).toUpperCase()}</div>
                            <div>
                                <strong>${i.nombre}</strong>
                                <span>${i.codigo}</span>
                            </div>
                        </div>
                    `);
                });
            }
        });
}

function cerrarModalIncidencia() {
    document.getElementById('modalDetalleIncidencia').style.display = 'none';
}
let chartInstance = null;
let datosEstadisticas = null;

function abrirGrafico(tipo) {
    // 1. Mostrar el modal
    const modal = document.getElementById('modalGrafico');
    modal.style.display = 'flex'; 

    // 2. Título dinámico inicial
    const titulo = document.getElementById('tituloGrafico');
    if (tipo === 'reservas') titulo.innerText = 'Reservas por mes';
    if (tipo === 'horas') titulo.innerText = 'Horas de laboratorio por mes';
    if (tipo === 'materiales') titulo.innerText = 'Materiales solicitados por mes';

    // 3. Obtener el mes actual para la consulta
    const urlParams = new URLSearchParams(window.location.search);
    const mesActual = urlParams.get('mes') || new Date().toISOString().slice(0, 7); 

    // 4. Cargar los datos reales desde el backend
    fetch(`/alumno/api/estadisticas?mes=${mesActual}`)
        .then(res => res.json())
        .then(data => {
            datosEstadisticas = data;
            dibujarGraficoConDatos(tipo); // Llamamos a la función que dibuja
        })
        .catch(() => {
            alert("Error al cargar las estadísticas.");
            cerrarGrafico();
        });
}

function dibujarGraficoConDatos(tipo) {
    // 1. Si ya hay un gráfico, lo destruimos para que no se superponga
    if (chartInstance) {
        chartInstance.destroy();
    }

    // 2. Esperamos un poquito a que el modal se pinte en pantalla (evita deformaciones)
    setTimeout(() => {
        const ctx = document.getElementById('canvasGrafico').getContext('2d');
        
        let datos = [];
        let color = '';

        if (tipo === 'reservas') {
            datos = datosEstadisticas.reservas;
            color = '#2563eb';
        } else if (tipo === 'horas') {
            datos = datosEstadisticas.horas;
            color = '#8b5cf6';
        } else {
            datos = datosEstadisticas.materiales;
            color = '#f59e0b';
        }

        // 3. Dibujamos el gráfico
        chartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: datosEstadisticas.labels,
                datasets: [{
                    label: 'Total',
                    data: datos,
                    backgroundColor: color,
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { 
                    legend: { display: false } 
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1 }
                    }
                }
            }
        });
    }, 100); // 100ms de delay para que el CSS haga su magia
}

function cerrarGrafico() {
    document.getElementById('modalGrafico').style.display = 'none';
}