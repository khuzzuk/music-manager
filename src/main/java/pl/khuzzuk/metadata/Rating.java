package pl.khuzzuk.metadata;

public enum Rating {
    UNDEFINED(0, 0, 0, 0, 0, 0, 0),
    NONE(1, 1, 14, 1, 1, 1, 0),
    HALF(15, 15, 30, 2, 9, 5, 1),
    ONE(31, 31, 31, 10, 19, 10, 2),
    ONE_HALF(32, 32, 48, 20, 29, 20, 3),
    TWO(64, 49, 64, 30, 39, 30, 4),
    TWO_HALF(96, 65, 96, 40, 49, 40, 5),
    THREE(128, 97, 159, 50, 59, 50, 6),
    THREE_HALF(160, 160, 195, 60, 69, 60, 7),
    FOUR(196, 196, 223, 70, 79, 70, 8),
    FOUR_HALF(224, 224, 251, 80, 89, 80, 9),
    FIVE(255, 252, 255, 90, 100, 99, 10);
    private final int value;
    private final int min;
    private final int max;
    private final int minP;
    private final int maxP;
    private final int valueP;
    private final int rate;

    Rating(int value, int min, int max, int minP, int maxP, int valueP, int rate) {
        this.value = value;
        this.min = min;
        this.max = max;
        this.minP = minP;
        this.maxP = maxP;
        this.valueP = valueP;
        this.rate = rate;
    }

    public static Rating fromByte(int value) {
        for (Rating rating : values()) {
            if (value >= rating.min && value <= rating.max) {
                return rating;
            }
        }

        return UNDEFINED;
    }

    public static Rating fromP(int value) {
        for (Rating rating : values()) {
            if (value >= rating.minP && value <= rating.maxP) {
                return rating;
            }
        }

        return UNDEFINED;
    }

    public static Rating fromRate(int rate) {
        for (Rating rating : values()) {
            if (rating.rate == rate) {
                return rating;
            }
        }

        return UNDEFINED;
    }

    public int getValueP() {
        return valueP;
    }

    public int getValue() {
        return value;
    }

    public int getRate() {
        return rate;
    }
}
