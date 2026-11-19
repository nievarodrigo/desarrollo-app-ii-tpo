package ar.edu.desarrolloappii.ecommerce.ia.service;

import ar.edu.desarrolloappii.ecommerce.ia.domain.SalidaModelo;

/**
 * Puerto hacia el proveedor de IA (patrón Adapter). Cambiar de Claude a otro proveedor
 * (DeepSeek, MiniMax, un modelo local...) es escribir otra implementación de esta
 * interfaz: el gateway, el controller y el front no se enteran.
 */
public interface ModeloIA {
    /** Nombre para logs y métricas, p. ej. "anthropic/claude-haiku-5-5". */
    String nombre();

    /** ¿Hay credenciales y configuración para usarlo? Si no, el gateway va directo al fallback. */
    boolean disponible();

    /** Interpreta la consulta. Puede tardar, fallar o devolver basura: el gateway se defiende de todo eso. */
    SalidaModelo interpretar(String consulta) throws Exception;
}
