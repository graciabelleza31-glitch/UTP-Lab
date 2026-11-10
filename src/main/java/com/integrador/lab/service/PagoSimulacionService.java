package com.integrador.lab.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.integrador.lab.modelo.PlinSimulacion;
import com.integrador.lab.modelo.TarjetaSimulacion;
import com.integrador.lab.modelo.YapeSimulacion;
import com.integrador.lab.repository.PlinSimulacionRepository;
import com.integrador.lab.repository.TarjetaSimulacionRepository;
import com.integrador.lab.repository.YapeSimulacionRepository;

@Service
public class PagoSimulacionService {

    @Autowired private TarjetaSimulacionRepository tarjetaRepo;
    @Autowired private YapeSimulacionRepository yapeRepo;
    @Autowired private PlinSimulacionRepository plinRepo;

    private static final Random RANDOM = new Random();

    public static class ResultadoPago {
        private final boolean exitoso;
        private final String mensaje;
        private final String transaccion;
        public ResultadoPago(boolean exitoso, String mensaje, String transaccion) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.transaccion = transaccion;
        }
        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        public String getTransaccion() { return transaccion; }
    }

    // Firma extendida (con Yape y Plin)
    public ResultadoPago procesarPago(
            String metodoPago,
            String numeroTarjeta,
            String titular,
            String fechaExp,
            String cvv,
            String numeroYape,
            String codigoYape,
            String numeroPlin,
            String codigoPlin,
            BigDecimal monto) {

        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultadoPago(true, "Reserva gratuita confirmada.", generarTransaccion("FREE"));
        }

        String metodo = (metodoPago == null) ? "" : metodoPago.trim().toUpperCase();
        if (metodo.contains(",")) metodo = metodo.split(",")[0].trim();

        System.out.println("🔍 [PAGO] metodo=[" + metodo + "] monto=" + monto);

        switch (metodo) {
            case "YAPE":    return procesarYape(numeroYape, codigoYape);
            case "PLIN":    return procesarPlin(numeroPlin, codigoPlin);
            case "TARJETA": return procesarTarjeta(numeroTarjeta, titular, fechaExp, cvv);
            case "GRATUITO":
            case "DIRECTO": return new ResultadoPago(true, "Reserva confirmada sin costo.", generarTransaccion("FREE"));
            default:        return new ResultadoPago(false, "Método de pago no reconocido: " + metodo, null);
        }
    }

    // =====================================================================
    // YAPE
    // =====================================================================
    private ResultadoPago procesarYape(String numeroYape, String codigoYape) {
        if (numeroYape == null || numeroYape.trim().isEmpty())
            return new ResultadoPago(false, "Ingresa el número de Yape.", null);

        String num = numeroYape.replaceAll("[^0-9]", "").trim();
        if (!num.matches("\\d{9}"))
            return new ResultadoPago(false, "El número de Yape debe tener 9 dígitos.", null);

        if (codigoYape == null || !codigoYape.matches("\\d{6}"))
            return new ResultadoPago(false, "Ingresa el código de aprobación de 6 dígitos.", null);

        Optional<YapeSimulacion> opt = yapeRepo.findByNumeroCelular(num);
        if (opt.isEmpty())
            return new ResultadoPago(false, "El número de Yape no está registrado en el sistema.", null);

        YapeSimulacion y = opt.get();
        String estado = y.getEstado() == null ? "" : y.getEstado().trim().toUpperCase();
        switch (estado) {
            case "ACTIVO":
                if (!y.getCodigoAprobacion().equals(codigoYape.trim()))
                    return new ResultadoPago(false, "El código de aprobación es incorrecto. Verifica el SMS de Yape.", null);
                return new ResultadoPago(true, "Pago con Yape aprobado correctamente.", generarTransaccion("YAPE"));
            case "EXPIRADO": return new ResultadoPago(false, "El código de aprobación expiró. Solicita uno nuevo.", null);
            case "BLOQUEADO": return new ResultadoPago(false, "Tu cuenta de Yape está bloqueada temporalmente.", null);
            case "NO_EXISTE": return new ResultadoPago(false, "El número de Yape no está afiliado al sistema.", null);
            default: return new ResultadoPago(false, "Estado de Yape desconocido: " + estado, null);
        }
    }

    // =====================================================================
    // PLIN (aquí está la clave: el código del QR + número de celular)
    // =====================================================================
    private ResultadoPago procesarPlin(String numeroPlin, String codigoPlin) {
        if (numeroPlin == null || numeroPlin.trim().isEmpty())
            return new ResultadoPago(false, "Ingresa el número de Plin.", null);

        String num = numeroPlin.replaceAll("[^0-9]", "").trim();
        if (!num.matches("\\d{9}"))
            return new ResultadoPago(false, "El número de Plin debe tener 9 dígitos.", null);

        if (codigoPlin == null || codigoPlin.trim().isEmpty())
            return new ResultadoPago(false, "Ingresa el código de operación que aparece en el QR.", null);

        Optional<PlinSimulacion> opt = plinRepo.findByNumeroCelular(num);
        if (opt.isEmpty())
            return new ResultadoPago(false, "El número de Plin no está registrado en el sistema.", null);

        PlinSimulacion p = opt.get();
        String estado = p.getEstado() == null ? "" : p.getEstado().trim().toUpperCase();
        switch (estado) {
            case "ACTIVO":
                if (!p.getCodigoOperacion().equalsIgnoreCase(codigoPlin.trim()))
                    return new ResultadoPago(false, "El código de operación del QR es incorrecto.", null);
                return new ResultadoPago(true, "Pago con Plin aprobado correctamente.", generarTransaccion("PLIN"));
            case "EXPIRADO": return new ResultadoPago(false, "El QR de Plin expiró. Genera uno nuevo.", null);
            case "BLOQUEADO": return new ResultadoPago(false, "Tu cuenta de Plin está bloqueada temporalmente.", null);
            case "NO_EXISTE": return new ResultadoPago(false, "El número de Plin no está afiliado al sistema.", null);
            default: return new ResultadoPago(false, "Estado de Plin desconocido: " + estado, null);
        }
    }

    // =====================================================================
    // TARJETA
    // =====================================================================
    private ResultadoPago procesarTarjeta(String numero, String titular, String fechaExp, String cvv) {
        if (numero == null || numero.trim().isEmpty())
            return new ResultadoPago(false, "Ingresa el número de tarjeta.", null);
        String num = numero.replaceAll("[^0-9]", "").trim();
        if (!num.matches("\\d{16}"))
            return new ResultadoPago(false, "El número de tarjeta debe tener 16 dígitos.", null);
        if (titular == null || titular.trim().isEmpty())
            return new ResultadoPago(false, "Ingresa el nombre del titular.", null);
        if (fechaExp == null || !fechaExp.matches("\\d{2}/\\d{2}"))
            return new ResultadoPago(false, "Fecha de expiración inválida. Usa MM/AA.", null);
        if (cvv == null || !cvv.matches("\\d{3,4}"))
            return new ResultadoPago(false, "CVV inválido.", null);

        Optional<TarjetaSimulacion> opt = tarjetaRepo.findByNumeroTarjeta(num);
        if (opt.isEmpty())
            return new ResultadoPago(false, "La tarjeta no está registrada en el sistema.", null);

        TarjetaSimulacion t = opt.get();
        if (!normalizar(t.getTitular()).equals(normalizar(titular)))
            return new ResultadoPago(false, "El titular no coincide con la tarjeta registrada.", null);
        if (!t.getFechaExpiracion().trim().equals(fechaExp.trim()))
            return new ResultadoPago(false, "La fecha de expiración no coincide con la tarjeta registrada.", null);
        if (!t.getCvv().trim().equals(cvv.trim()))
            return new ResultadoPago(false, "El CVV no coincide con la tarjeta registrada.", null);

        String estado = t.getEstado() == null ? "" : t.getEstado().trim().toUpperCase();
        switch (estado) {
            case "ACEPTADA": case "ACTIVA":
                return new ResultadoPago(true, "Pago aprobado correctamente.", generarTransaccion("CARD"));
            case "RECHAZADA_FONDOS": return new ResultadoPago(false, "Fondos insuficientes en la tarjeta.", null);
            case "RECHAZADA_NO_EXISTE": return new ResultadoPago(false, "Tarjeta no válida o no autorizada por el banco.", null);
            case "BLOQUEADA": return new ResultadoPago(false, "Tarjeta bloqueada. Contacta a tu banco.", null);
            case "VENCIDA": return new ResultadoPago(false, "Tarjeta vencida.", null);
            default: return new ResultadoPago(false, "Estado de tarjeta desconocido: " + estado, null);
        }
    }

    private String normalizar(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    private String generarTransaccion(String prefijo) {
        long ts = System.currentTimeMillis();
        int rand = 1000 + RANDOM.nextInt(9000);
        return prefijo + "-" + ts + "-" + rand;
    }
}