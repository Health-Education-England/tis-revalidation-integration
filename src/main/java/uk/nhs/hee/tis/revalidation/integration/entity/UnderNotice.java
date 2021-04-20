package uk.nhs.hee.tis.revalidation.integration.entity;

public enum UnderNotice {

    YES("Yes"), NO("No"), ON_HOLD("On Hold");

    private final String notice;

    UnderNotice(final String notice) {
        this.notice = notice;
    }

    public String value() {
        return notice;
    }

    public static UnderNotice fromString(final String value) {
        for (final UnderNotice underNotice : values()) {
            if (underNotice.notice.equalsIgnoreCase(value)) {
                return underNotice;
            }
        }
        return null;
    }

}
