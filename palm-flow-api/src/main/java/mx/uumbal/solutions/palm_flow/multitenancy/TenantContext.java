package mx.uumbal.solutions.palm_flow.multitenancy;

/**
 * Thread-local holder for the current tenant identifier (empresa slug).
 * Must be set before any JPA operation on tenant-scoped entities
 * and cleared after the request/operation completes.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
