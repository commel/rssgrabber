package de.holarse.rssgrabber.output;

public class OutputUtils {
    
    public static String sanatize(final String input) {
        return sanatize(input, TitleAdjustOption.KEEP_TRAILING_WHITESPACE);
    }
    
    public static String sanatize(final String input, final TitleAdjustOption titleAdjustOption) {
        String output = input;

        // Nur Ascii-Zeichen erlauben, keine Emojis mehr
        final StringBuilder buffer = new StringBuilder();
        for (final char c: output.toCharArray()) {
            if (c <= 0xFF) {
                buffer.append(c);
            }
        }
        output = buffer.toString();        
        
        // Trim
        output = output.trim();
        
        // Newlines raus
        output = output.replace("\n", " ").replace("\r", "");
        
        // Wenn ein Doppelpunkt gefunden wird, sicherstellen, dass am Anfang ein Whitespace ist, weil Drupal mag das so lieber
        if (titleAdjustOption == TitleAdjustOption.KEEP_TRAILING_WHITESPACE) {
            if (output.contains(":") && !output.startsWith(" ")) {
                output = " " + output;
            }
        }
        
        return output;
    }
    
    private OutputUtils() {}
    
}
