package ar.edu.desarrolloappii.ecommerce.ia.service;

/**
 * Puerto hacia el proveedor de IA (patrón Adapter). Cambiar de Claude a otro proveedor
 * (DeepSeek, MiniMax, un modelo local...) es escribir otra implementación de esta
 * interfaz: el gateway, el controller y el front no se enteran.
 */
public interface ModeloIA {
    /** Nombre para logs y métricas, p. ej. "anthropic/claude-haiku-5-5". */
    String nombre();

    /** Versión del prompt que usa, p. ej. "busqueda-v2" (se registra en cada traza). */
    default String versionPrompt() { return "-"; }

    /** Precio en US$ por millón de tokens de entrada y de salida, para estimar el costo de cada llamada. */
    default double precioEntradaPorMillon() { return 0; }
    default double precioSalidaPorMillon() { return 0; }

    /** ¿Hay credenciales y configuración para usarlo? Si no, el gateway va directo al fallback. */
    boolean disponible();

    /** Interpreta la consulta. Puede tardar, fallar o devolver basura: el gateway se defiende de todo eso. */
    ResultadoModelo interpretar(String consulta) throws Exception;
}
