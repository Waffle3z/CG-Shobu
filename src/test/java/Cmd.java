import java.util.ArrayList;
import java.util.List;

/** Splits a command line on spaces outside double quotes (the SDK's String overload splits on every space). */
final class Cmd {
    static String[] split(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (char c : s.toCharArray()) {
            if (c == '"') quoted = !quoted;
            else if (c == ' ' && !quoted) { if (cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); } }
            else cur.append(c);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
