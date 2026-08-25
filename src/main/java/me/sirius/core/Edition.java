package me.sirius.core;

public final class Edition {

    private static final String EDITION = readEdition();

    private Edition() {
    }

    private static String readEdition() {
        try (var stream = Edition.class.getResourceAsStream("/edition")) {
            if (stream == null) {
                return "FREE";
            }
            String value = new String(stream.readAllBytes()).trim();
            return value.isEmpty() ? "FREE" : value;
        } catch (Exception e) {
            return "FREE";
        }
    }

    public static String current() {
        return EDITION;
    }

    public static boolean isPremium() {
        return "PREMIUM".equals(EDITION);
    }
}
