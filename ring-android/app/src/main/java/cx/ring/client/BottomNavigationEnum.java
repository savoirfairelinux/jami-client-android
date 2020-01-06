package cx.ring.client;

public enum BottomNavigationEnum {
    CONTACT_REQUESTS(0),
    CONVERSATIONS(1),
    ACCOUNT(2);

    private int mValue;

    BottomNavigationEnum(int value) {
        mValue = value;
    }

    public int getValue() {
        return mValue;
    }

}
