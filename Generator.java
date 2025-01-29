import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
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
    }

    private static String[] order = {
            "Townsfolk",
            "Outsider",
            "Minion",
            "Demon",
            "Fabled",
            "Traveller",
            "Potion"
    };

    private static void walk(CharacterDirectory characterDirectory, File directory) throws IOException {
        if (new File(directory, "image.png").exists()) {
            BufferedImage image = ImageIO.read(new File(directory, "image.png"));
            image = cropSquare(image);

            Image image1 = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            image = new BufferedImage(image1.getWidth(null), image1.getHeight(null), BufferedImage.TYPE_INT_ARGB);

            Graphics g = image.getGraphics();
            g.drawImage(image1, 0, 0, null);
            g.dispose();

            ImageIO.write(image, "png", new File(directory, "image_readme.png"));
        }

        if (new File(directory, "character.json").exists()) {
            Character character = new Character();
            character.file = directory;

            if (new File(directory, "image_readme.png").exists()) {
                character.image = new File(directory, "image_readme.png");
            } else {
                character.image = new File(directory.getParentFile(), "image_readme.png");
            }

            String json = new BufferedReader(new FileReader(new File(directory, "character.json")))
                    .lines()
                    .collect(Collectors.joining(""));
            Matcher matcher = Pattern.compile("\"ability\" *: *\"([^\"]*)\"").matcher(json.replace("\\\"", "\\'"));
            matcher.results().forEach(matchResult -> {
                character.ability = json.substring(matchResult.start(1), matchResult.end(1));
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
            bufferedWriter.write("## ![](" + character.image.getPath().substring(file.getPath().length() + 1).replace(" ", "%20").replace('\\', '/') + ") [" + character.file.getName() + "](" + character.file.getName().replace(" ", "%20").replace('\\', '/') + ")\n");
            bufferedWriter.write(character.ability + "\n");
            bufferedWriter.write("\n");
        }

        bufferedWriter.close();

        for (CharacterDirectory directory : characterDirectory.directoryList) {
            generate(directory, new File(file, directory.file.getName()));
        }
    }

    private static void generate(CharacterDirectory parent, CharacterDirectory characterDirectory, BufferedWriter bufferedWriter, int depth) throws IOException {
        bufferedWriter.write("#".repeat(depth) + " [" + characterDirectory.file.getName() + "](" + characterDirectory.file.getPath().substring(parent.file.getPath().length() + 1).replace(" ", "%20").replace('\\', '/') + ") (" + characterDirectory.getCharacterCount() + ")\n");
        for (Character character : characterDirectory.characterList) {
            bufferedWriter.write("- ![](" + character.image.getPath().substring(parent.file.getPath().length() + 1).replace(" ", "%20").replace('\\', '/') + ") [" + character.file.getName() + "](" + character.file.getPath().substring(parent.file.getPath().length() + 1).replace(" ", "%20").replace('\\', '/') + ")\n");
        }
        for (CharacterDirectory dir : characterDirectory.directoryList) {
            generate(parent, dir, bufferedWriter, depth + 1);
        }
        bufferedWriter.write("\n");
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
