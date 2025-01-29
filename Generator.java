import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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

        if (Arrays.stream(directory.listFiles()).anyMatch(t -> t.isFile() && t.getName().endsWith(".json"))) {
            bufferedWriter.write("- ");
            if (new File(directory, "image.png").exists()) {
                bufferedWriter.write("![](" + link.replace('\\', '/') + "/image_readme.png) ");
            } else {
                String linkCopy = link.replace('\\', '/');
                linkCopy = linkCopy.substring(0, linkCopy.lastIndexOf('/'));
                bufferedWriter.write("![](" + linkCopy + "/image_readme.png) ");
            }

            bufferedWriter.write("[" + directory.getName() + "](https://github.com/yoyosource/BOTC-HomeBrew/tree/master/" + link.replace('\\', '/') + ")");
            bufferedWriter.newLine();
            return;
        }

        bufferedWriter.newLine();
        long numberOfCharacters = Files.walk(directory.toPath()).map(Path::toFile).filter(file -> file.getName().equals("character.json")).count();
        bufferedWriter.write("#".repeat(depth) + " " + directory.getName() + " (" + numberOfCharacters + ")" + "\n");

        File[] files = directory.listFiles();
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            if (file.isDirectory()) {
                generate(file, depth + 1);
            }
        }
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
                } else {
                    value = true ? 0 : alpha.getPixel(x, y, new int[1])[0];
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
