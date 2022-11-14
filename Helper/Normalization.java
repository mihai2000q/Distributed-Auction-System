public abstract class Normalization {
    public static String normalizeString(String str) {
        str = normalizeUsername(str);
        var firstLetter = String.valueOf(str.charAt(0)).toUpperCase();
        return firstLetter + str.substring(1);
    }
    public static String normalizeUsername(String str) {
        return str.toLowerCase().stripTrailing().stripIndent().stripLeading();
    }
}
