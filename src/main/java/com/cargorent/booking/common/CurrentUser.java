package com.cargorent.booking.common;

/**
 * The caller identity, taken from the X-User-* headers that the API gateway adds after
 * validating the JWT. When the headers are absent the call came from another service
 * (internal call), never from a browser, because the gateway always strips and re-adds them.
 */
public record CurrentUser(Long id, String role, String name, String email) {

    public boolean isAuthenticated() { return id != null; }

    public boolean isAdmin() { return "ADMIN".equals(role); }

    public boolean isInternal() { return id == null; }

    public Long requireId() {
        if (id == null) throw ApiException.unauthorized("Login required");
        return id;
    }

    public void requireAdmin() {
        requireId();
        if (!isAdmin()) throw ApiException.forbidden("Admin access required");
    }
}
