public final class Validation {
    private Validation() {}
    public static int validateInteger(String number) {
        try {
            return Integer.parseInt(number);
        }
        catch (NumberFormatException exception) {
            System.out.println("Please insert a number instead!!!");
            return 0;
        }
    }
}
