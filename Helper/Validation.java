public final class Validation {
    private Validation() {}
    public static int validateInteger(String number) {
        try {
            var num = Integer.parseInt(number);
            if(num < 0)
                throw new NegativeNumberException();
            return num;
        }
        catch (NumberFormatException exception) {
            System.out.println("\nPlease insert a number instead!!!");
            return -1;
        }
        catch (NegativeNumberException exception) {
            System.out.println("\nPlease insert only positive numbers");
            return -1;
        }
    }
}
