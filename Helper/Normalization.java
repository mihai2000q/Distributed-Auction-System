import java.text.DecimalFormat;

public final class Normalization {
    private Normalization() {}
    public static String normalizeString(String str) {
        str = normalizeUsername(str);
        if(str.isEmpty() || str.isBlank())
            return "";
        var firstLetter = String.valueOf(str.charAt(0)).toUpperCase();
        return firstLetter + str.substring(1);
    }
    public static String normalizeUsername(String str) {
        var result = str.toLowerCase().stripTrailing().stripIndent().stripLeading();
        return result == null ? "" : result;
    }
}
