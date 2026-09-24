import java.util.HashMap;
import java.util.Map;

public class Cipher {
    // maps each ciphered character back to its real character
    private final Map<Character, Character> map = new HashMap<>();

    public Cipher(String key) {
        if (key == null || key.isBlank()) {
            return; // empty key: text is left unchanged
        }
        String[] lines = key.strip().split("\\R");
        if (lines.length != 2 || lines[0].length() != lines[1].length()) {
            throw new IllegalArgumentException("Invalid key: must be 2 lines of the same length");
        }
        for (int i = 0; i < lines[0].length(); i++) {
            map.put(lines[1].charAt(i), lines[0].charAt(i));
        }
    }

    public String decipher(String text) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            result.append(map.getOrDefault(c, c));
        }
        return result.toString();
    }
}