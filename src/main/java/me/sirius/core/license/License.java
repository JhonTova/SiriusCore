package me.sirius.core.license;

import java.time.Instant;
import java.util.Set;

public record License(
        int version,
        String buyer,
        String buyerId,
        Set<String> products,
        Instant issuedAt,
        Instant expiresAt,
        String nonce
) {

    public static final Instant PERPETUAL = Instant.parse("2999-12-31T23:59:59Z");

    public boolean isPerpetual() {
        return expiresAt.equals(PERPETUAL) || expiresAt.isAfter(PERPETUAL.minusSeconds(1));
    }

    public boolean isExpired() {
        return !isPerpetual() && Instant.now().isAfter(expiresAt);
    }

    public boolean covers(String product) {
        return products.contains("*") || products.contains(product.toLowerCase());
    }

    public String watermark() {
        return nonce.length() <= 8 ? nonce : nonce.substring(0, 8);
    }
}
