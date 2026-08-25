package me.sirius.core.license;

public enum LicenseStatus {

    VALID("Licencia válida"),

    FREE("Edición gratuita"),

    MISSING("Falta el archivo license.key"),

    MALFORMED("El archivo license.key está corrupto o mal formado"),

    FORGED("La firma de la licencia no es válida"),

    EXPIRED("La licencia ha caducado"),

    NOT_COVERED("La licencia no cubre este plugin"),

    TAMPERED("La integridad del core ha sido comprometida");

    private final String reason;

    LicenseStatus(String reason) {
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }

    public boolean allowsStartup() {
        return this == VALID || this == FREE;
    }
}
