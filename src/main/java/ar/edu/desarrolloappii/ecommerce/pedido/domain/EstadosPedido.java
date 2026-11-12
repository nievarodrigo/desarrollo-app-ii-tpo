package ar.edu.desarrolloappii.ecommerce.pedido.domain;

import java.util.Map;

/** Objetos State inmutables, compartidos entre pedidos. */
public final class EstadosPedido {
    private EstadosPedido() { }
    private abstract static class Base implements EstadoPedido {
        private final String nombre;
        Base(String nombre) { this.nombre = nombre; }
        @Override public String nombre() { return nombre; }
        @Override public EstadoPedido aplicar(EventoPedido evento) {
            throw new IllegalStateException("Evento " + evento + " inválido en " + nombre);
        }
    }
    public static final EstadoPedido PENDIENTE_VALIDACION = new Base("PENDIENTE_VALIDACION") {
        @Override public EstadoPedido aplicar(EventoPedido e) {
            return switch (e) {
                case RESERVA_ACEPTADA -> PENDIENTE;
                case RESERVA_RECHAZADA -> RECHAZADO;
                default -> super.aplicar(e);
            };
        }
    };
    public static final EstadoPedido PENDIENTE = new Base("PENDIENTE") {
        @Override public EstadoPedido aplicar(EventoPedido e) {
            return switch (e) {
                case MENSAJE_PROCESADO -> PROCESADO;
                case ENVIO_FALLIDO -> ERROR_ENVIO;
                default -> super.aplicar(e);
            };
        }
    };
    public static final EstadoPedido ERROR_ENVIO = new Base("ERROR_ENVIO") {
        @Override public EstadoPedido aplicar(EventoPedido e) {
            return switch (e) {
                case REINTENTAR_ENVIO -> PENDIENTE;
                case MENSAJE_PROCESADO -> PROCESADO; // entrega tardía tras envío ambiguo
                default -> super.aplicar(e);
            };
        }
    };
    public static final EstadoPedido PROCESADO = new Base("PROCESADO") {
        @Override public EstadoPedido aplicar(EventoPedido e) {
            return switch (e) {
                case MENSAJE_PROCESADO -> this; // redelivery sin segundo efecto
                case CONFIRMAR -> CONFIRMADO;
                default -> super.aplicar(e);
            };
        }
    };
    public static final EstadoPedido CONFIRMADO = new Base("CONFIRMADO") {
        @Override public EstadoPedido aplicar(EventoPedido e) {
            return e == EventoPedido.CONFIRMAR || e == EventoPedido.MENSAJE_PROCESADO
                    ? this : super.aplicar(e);
        }
    };
    public static final EstadoPedido RECHAZADO = new Base("RECHAZADO") { };
    private static final Map<String, EstadoPedido> ESTADOS = Map.of(
            "PENDIENTE_VALIDACION", PENDIENTE_VALIDACION, "PENDIENTE", PENDIENTE,
            "ERROR_ENVIO", ERROR_ENVIO, "PROCESADO", PROCESADO,
            "CONFIRMADO", CONFIRMADO, "RECHAZADO", RECHAZADO);

    public static EstadoPedido desde(String nombre) {
        EstadoPedido estado = ESTADOS.get(nombre);
        if (estado == null) throw new IllegalArgumentException("Estado desconocido: " + nombre);
        return estado;
    }
}
