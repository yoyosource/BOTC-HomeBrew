import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class Generator {

    private static BufferedWriter bufferedWriter;

    private static String[] order = {
            "Townsfolk",
            "Outsider",
            "Minion",
            "Demon",
            "Fabled",
            "Traveller",
            "Potion"
    };

    public static void main(String[] args) throws IOException {
        bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("README.md")));
        bufferedWriter.write("# HomeBrew\n");
        for (String s : order) {
            generate(new File(".", s), 2);
        }
        bufferedWriter.close();
    }

    private static void generate(File directory, int depth) throws IOException {
        if (directory.listFiles().length == 0) {
            return;
        }
        String link = directory.getPath().substring(2);
        link = link.replace(" ", "%20");

        if (new File(directory, "character.json").exists()) {
            bufferedWriter.write("- [" + directory.getName() + "](https://github.com/yoyosource/BOTC-HomeBrew/tree/master/" + link.replace('\\', '/') + ")");
            bufferedWriter.newLine();
            return;
        }

        bufferedWriter.newLine();
        // bufferedWriter.write("#".repeat(depth) + " [" + directory.getName() + "](https://github.com/yoyosource/BOTC-HomeBrew/tree/master/" + link + ")\n");
        bufferedWriter.write("#".repeat(depth) + " " + directory.getName() + "\n");

        File[] files = directory.listFiles();
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            if (file.isDirectory()) {
                generate(file, depth + 1);
            }
        }
    }
}
