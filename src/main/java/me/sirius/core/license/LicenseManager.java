package me.sirius.core.license;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

public final class LicenseManager {

    private static final String PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoEK5J8hvsBbVFPcbszIWuUkvEWBMtfvU"
          + "Q+zQ1865thGE/CG5x0uj+2D2Lc2Vsx/Qr8Xm1CgA7ECZ4WHUX9BJ1b29jbnNHNl4TLbW+MrxeQup"
          + "Y9Abmjp7qyyVkUjDguZ7uaxXPzoU4PefsSCFf4TrCwD7B4fY1CX32cARRG4l8BE2j3dqCmvuWciD"
          + "mXAcf8rmGynOI++FdvcsYUSy8IpzSvZOlqnd65lW9C/IOM7DvgDVwVbxWzhZM5Pt6SgZCqgt49Zq"
          + "9Ps92ffLWCvMXbWmphWm610MYBpCDGUYBpnjeZA6n3Q8Abjeu1c+2Xn2WEMUhRj2qbBM3AiK6aSS"
          + "fHtBMwIDAQAB";

    private static final int PAYLOAD_FIELDS = 7;
    private static final int SUPPORTED_VERSION = 1;

    private static final String LICENSE_TEMPLATE = """
            # ============================================================
            #  SiriusCore - Licencia
            # ============================================================
            #  Pega tu licencia en la ultima linea de este archivo, en UNA
            #  sola linea y sin espacios. La recibes al comprar en BuiltByBit.
            #
            #  Sin licencia, los plugins premium no arrancan. Los gratuitos
            #  funcionan igual.
            #
            #  Las lineas que empiezan por # son comentarios y se ignoran.
            # ============================================================

            """;

    private final Path licenseFile;

    private License license;
    private LicenseStatus status;

    public LicenseManager(Path dataFolder) {
        this.licenseFile = dataFolder.resolve("license.key");
    }

    public void load() {
        if (!Files.isRegularFile(licenseFile)) {
            createTemplate();
            this.status = LicenseStatus.MISSING;
            return;
        }
        try {
            String key = extractKey(Files.readString(licenseFile, StandardCharsets.UTF_8));
            this.status = key.isEmpty() ? LicenseStatus.MISSING : parse(key);
        } catch (IOException e) {
            this.status = LicenseStatus.MALFORMED;
        }
    }

    private String extractKey(String content) {
        for (String line : content.split("\n")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                return trimmed;
            }
        }
        return "";
    }

    private void createTemplate() {
        try {
            if (licenseFile.getParent() != null) {
                Files.createDirectories(licenseFile.getParent());
            }
            Files.writeString(licenseFile, LICENSE_TEMPLATE, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Si no se puede escribir, no es crítico: el usuario puede crearlo a mano.
        }
    }

    private LicenseStatus parse(String key) {
        String[] parts = key.split("\\.");
        if (parts.length != 2) {
            return LicenseStatus.MALFORMED;
        }

        byte[] payload;
        byte[] signature;
        try {
            Base64.Decoder decoder = Base64.getUrlDecoder();
            payload = decoder.decode(parts[0]);
            signature = decoder.decode(parts[1]);
        } catch (IllegalArgumentException e) {
            return LicenseStatus.MALFORMED;
        }

        if (!verifySignature(payload, signature)) {
            return LicenseStatus.FORGED;
        }

        String[] fields = new String(payload, StandardCharsets.UTF_8).split("\\|", -1);
        if (fields.length != PAYLOAD_FIELDS) {
            return LicenseStatus.MALFORMED;
        }

        try {
            int version = Integer.parseInt(fields[0]);
            if (version != SUPPORTED_VERSION) {
                return LicenseStatus.MALFORMED;
            }

            Set<String> products = new LinkedHashSet<>(Arrays.asList(fields[3].split(",")));
            this.license = new License(
                    version,
                    fields[1],
                    fields[2],
                    Set.copyOf(products),
                    Instant.ofEpochSecond(Long.parseLong(fields[4])),
                    Instant.ofEpochSecond(Long.parseLong(fields[5])),
                    fields[6]);
        } catch (NumberFormatException e) {
            return LicenseStatus.MALFORMED;
        }

        return license.isExpired() ? LicenseStatus.EXPIRED : LicenseStatus.VALID;
    }

    private boolean verifySignature(byte[] payload, byte[] signature) {
        try {
            byte[] der = Base64.getDecoder().decode(PUBLIC_KEY);
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));

            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public LicenseStatus check(String product) {
        if (status != LicenseStatus.VALID) {
            return status;
        }
        if (license.isExpired()) {
            return LicenseStatus.EXPIRED;
        }
        return license.covers(product) ? LicenseStatus.VALID : LicenseStatus.NOT_COVERED;
    }

    public LicenseStatus status() {
        return status;
    }

    public License license() {
        return status == LicenseStatus.VALID ? license : null;
    }

    public Path licenseFile() {
        return licenseFile;
    }
}
