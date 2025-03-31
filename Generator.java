import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Generator {

    private static class CharacterDirectory {
        private File file;
        private List<CharacterDirectory> directoryList = new ArrayList<>();
        private List<Character> characterList = new ArrayList<>();

        public int getCharacterCount() {
            return characterList.size() + directoryList.stream().mapToInt(CharacterDirectory::getCharacterCount).sum();
        }
    }

    private static class Character {
        private File file;
        private String ability;
        private File image;
        private Map<String, String> jinxes = new HashMap<>();
    }

    private static String[] order = {
            "Townsfolk",
            "Outsider",
            "Minion",
            "Demon",
            "Fabled",
            "Traveller",
            "Special",
            "Potion"
    };

    private static void walk(CharacterDirectory characterDirectory, File directory) throws IOException {
        if (new File(directory, "image.png").exists()) {
            BufferedImage image = ImageIO.read(new File(directory, "image.png"));
            image = cropSquare(image);

            {
                Image scaled = image.getScaledInstance(40, 40, Image.SCALE_AREA_AVERAGING);
                BufferedImage result = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB);

                Graphics g = result.getGraphics();
                g.drawImage(scaled, 0, 0, null);
                g.dispose();

                ImageIO.write(result, "png", new File(directory, ".image_big.png"));
            } // Scaled image 30x30
            {
                Image scaled = image.getScaledInstance(20, 20, Image.SCALE_AREA_AVERAGING);
                BufferedImage result = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB);

                Graphics g = result.getGraphics();
                g.drawImage(scaled, 0, 0, null);
                g.dispose();

                ImageIO.write(result, "png", new File(directory, ".image_small.png"));
            } // Scaled image 20x20
        }

        if (new File(directory, "character.json").exists()) {
            Character character = new Character();
            character.file = directory;

            if (new File(directory, "image.png").exists()) {
                character.image = directory;
            } else {
                character.image = directory.getParentFile();
            }

            String json = new BufferedReader(new FileReader(new File(directory, "character.json")))
                    .lines()
                    .collect(Collectors.joining(""));
            Pattern.compile("\"ability\" *: *\"([^\"]*)\"").matcher(json.replace("\\\"", "\\'"))
                    .results().forEach(matchResult -> {
                        character.ability = json.substring(matchResult.start(1), matchResult.end(1));
                    });

            Pattern.compile("\\{\\s*\"id\" *: *\"([^\"]+)\",\\s*\"reason\" *: *\"([^\"]+)\"}")
                    .matcher(json.replace("\\\"", "\\'"))
                    .results().forEach(matchResult -> {
                        String jinxWith = json.substring(matchResult.start(1), matchResult.end(1));
                        String reason = json.substring(matchResult.start(2), matchResult.end(2));
                        character.jinxes.put(jinxWith, reason);
                    });

            characterDirectory.characterList.add(character);
        } else {
            CharacterDirectory characterDir = new CharacterDirectory();
            characterDirectory.directoryList.add(characterDir);
            characterDir.file = directory;

            File[] files = directory.listFiles();
            if (files == null) return;

            for (File f : files) {
                if (!f.isDirectory()) continue;
                walk(characterDir, f);
            }
        }

        characterDirectory.directoryList.sort(Comparator.comparing(dir -> dir.file.getName()));
        characterDirectory.characterList.sort(Comparator.comparing(dir -> dir.file.getName()));
    }

    private static void generate(CharacterDirectory characterDirectory, File file) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(file, "README.md")));
        if (characterDirectory.file.getName().equals(".")) {
            bufferedWriter.write("# Homebrew\n");
        } else {
            bufferedWriter.write("# " + characterDirectory.file.getName() + "\n");
        }
        bufferedWriter.write("\n");

        for (CharacterDirectory dir : characterDirectory.directoryList) {
            generate(characterDirectory, dir, bufferedWriter, 2);
        }

        for (Character character : characterDirectory.characterList) {
            String imageBaseLink = toPath(character.image, file);
            bufferedWriter.write("## ![](" + imageBaseLink + (!imageBaseLink.isEmpty() ? "/" : "") + ".image_big.png) [" + character.file.getName() + "](" + character.file.getName().replace(" ", "%20").replace('\\', '/') + ")\n");
            bufferedWriter.write(character.ability + "\n");
            bufferedWriter.write("\n");

            generateCharacterReadme(character);
        }

        bufferedWriter.close();

        for (CharacterDirectory directory : characterDirectory.directoryList) {
            generate(directory, new File(file, directory.file.getName()));
        }
    }

    private static void generateCharacterReadme(Character character) throws IOException {
        File file = new File(character.file, "README.md");
        if (file.exists()) {
            if (true) return; // TODO: Implement it properly!
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            List<String> data = new ArrayList<>(bufferedReader.lines().collect(Collectors.toList()));
            bufferedReader.close();

            int summaryLine = 0;
            int jinxLine = 0;
            for (int i = 0; i < data.size(); i++) {
                String line = data.get(i);

                if (line.equals("## Summary")) {
                    summaryLine = i;
                }
                if (i == summaryLine + 1 && line.startsWith("> ")) {
                    data.set(i, "> " + character.ability);
                }

                if (line.equals("## Jinxes")) {
                    jinxLine = i;
                }
                if (i > jinxLine && jinxLine != 0) {
                    if (line.startsWith("## ")) {
                        if (character.jinxes.isEmpty()) {
                            data.remove(jinxLine);
                            i--;
                        }
                        jinxLine = 0;
                        continue;
                    }
                    data.remove(jinxLine + 1);
                }
            }

            if (!character.jinxes.isEmpty()) {
                for (Map.Entry<String, String> entry : character.jinxes.entrySet()) {
                    data.add("### " + entry.getKey());
                    data.add(entry.getValue());
                }
            }

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            for (String line : data) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
            return;
        }

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("# " + character.file.getName() + "\n");
        bufferedWriter.write("<img src=\"https://raw.githubusercontent.com/yoyosource/BOTC-HomeBrew/master/" + character.image.getPath().substring(2) + "/image.png\" alt=\"drawing\" width=\"200\"/>\\\n");
        bufferedWriter.write("Authors: \n");
        bufferedWriter.write("\n");
        bufferedWriter.write("## Summary\n");
        bufferedWriter.write("> " + character.ability + "\n");
        bufferedWriter.write("\n");
        if (!character.jinxes.isEmpty()) {
            bufferedWriter.write("## Jinxes\n");
            for (Map.Entry<String, String> entry : character.jinxes.entrySet()) {
                bufferedWriter.write("### " + entry.getKey() + "\n");
                bufferedWriter.write(entry.getValue() + "\n");
            }
        }
        bufferedWriter.close();
    }

    private static void generate(CharacterDirectory parent, CharacterDirectory characterDirectory, BufferedWriter bufferedWriter, int depth) throws IOException {
        bufferedWriter.write("#".repeat(depth) + " [" + characterDirectory.file.getName() + "](" + toPath(characterDirectory.file, parent.file) + ") (" + characterDirectory.getCharacterCount() + ")\n");
        for (Character character : characterDirectory.characterList) {
            bufferedWriter.write("- ![](" + toPath(character.image, parent.file) + "/.image_small.png) [" + character.file.getName() + "](" + toPath(character.file, parent.file) + ")\n");
        }
        for (CharacterDirectory dir : characterDirectory.directoryList) {
            generate(parent, dir, bufferedWriter, depth + 1);
        }
        bufferedWriter.write("\n");
    }

    private static String toPath(File pathToGenerate, File parent) {
        String path = pathToGenerate.getPath().substring(parent.getPath().length());
        if (path.startsWith("/")) path = path.substring(1);
        return path.replace(" ", "%20").replace('\\', '/');
    }

    public static void main(String[] args) throws IOException {
        CharacterDirectory characterDirectory = new CharacterDirectory();
        walk(characterDirectory, new File("."));
        characterDirectory = characterDirectory.directoryList.get(0);
        characterDirectory.directoryList.removeIf(dir -> {
            for (String order : order) {
                if (dir.file.getName().equals(order)) return false;
            }
            return true;
        });
        characterDirectory.directoryList.sort(Comparator.comparing(dir -> {
            for (int i = 0; i < order.length; i++) {
                if (dir.file.getName().equals(order[i])) return i;
            }
            return -1;
        }));
        characterDirectory.file = new File(".");

        generate(characterDirectory, new File("."));
    }

    public static BufferedImage cropSquare(BufferedImage image) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        WritableRaster alpha = image.getAlphaRaster();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int value = image.getRGB(x, y) >>> 24;
                if (value > 100) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                } else if (alpha != null) {
                    value = alpha.getPixel(x, y, new int[1])[0];
                    if (value > 100) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }
                }
            }
        }
        int neededWidth = maxX - minX;
        int neededHeight = maxY - minY;
        int squareSize = Math.max(neededHeight, neededWidth);
        int xCompensation = (neededWidth - squareSize) / 2;
        int yCompensation = (neededHeight - squareSize) / 2;
        minX = Math.max(minX + xCompensation, 0);
        minY = Math.max(minY + yCompensation, 0);
        return image.getSubimage(minX, minY, squareSize, squareSize);
    }
}
