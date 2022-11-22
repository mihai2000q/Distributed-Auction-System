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
            System.out.println("Please insert a number instead!!!");
            throw new RuntimeException(exception);
        }
        catch (NegativeNumberException exception) {
            System.out.println("Please insert only positive numbers");
            throw new RuntimeException(exception);
        }
    }
}
