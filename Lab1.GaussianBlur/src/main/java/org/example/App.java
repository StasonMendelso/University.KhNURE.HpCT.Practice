package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hello world!
 */
public class App {
    public static final int NUMBER_OF_THREADS = 12;

    public static void main(String[] args) throws IOException {
        InputStream inputStream = App.class.getClassLoader().getResourceAsStream("enot.png");
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        int height = bufferedImage.getHeight();
        int width = bufferedImage.getWidth();
        int[][] pixels = convertToArray(bufferedImage);

        double[][] gaussianKernel = GaussianBlurExecutor.createGaussianKernel(23, 10);
        int[][] resultMatrix = new int[height][width];
        long startTime = System.currentTimeMillis();

        int remainHeight = height, startY = 0,  stripHeight = height / NUMBER_OF_THREADS, endY = startY + stripHeight - 1;
        int kernelHalf = gaussianKernel.length / 2;
        List<GaussianImageWorker> workers = new ArrayList<>();
        for (int threadNumber = 1; threadNumber <= NUMBER_OF_THREADS; threadNumber++) {
            int upperBuffer = Math.min(kernelHalf, startY);
            int lowerBuffer = Math.min(kernelHalf, height - endY - 1);

            int[][] strippedPixels = new int[stripHeight + upperBuffer + lowerBuffer][];
            int pointer = startY - upperBuffer;
            for(int index = 0; index < strippedPixels.length; index++, pointer++){
                strippedPixels[index] = Arrays.copyOf(pixels[pointer], width);
            }

            Point startPoint = new Point(0, upperBuffer);
            Point endPoint = new Point(width - 1, upperBuffer + stripHeight - 1);

            GaussianImageWorker worker = new GaussianImageWorker(resultMatrix, gaussianKernel, strippedPixels, startPoint, endPoint, startY);
            worker.start();
            workers.add(worker);

            remainHeight -= stripHeight;
            if (remainHeight <= 2 * stripHeight && threadNumber + 1 == NUMBER_OF_THREADS) {
                stripHeight += remainHeight - stripHeight;
            }
            startY = endY + 1;
            endY = startY + stripHeight - 1;
        }
        workers.forEach(gaussianImageWorker -> {
            try {
                gaussianImageWorker.join();
            } catch (InterruptedException e) {
                System.out.println("Can't wait finishing of a thread!");
                throw new RuntimeException(e);
            }
        });


        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                resultImage.setRGB(x, y, resultMatrix[y][x]);
            }
        }
        long endTime = System.currentTimeMillis();

        System.out.println("Кількість потоків = " +  NUMBER_OF_THREADS + ". Час виконання = " + (endTime - startTime) + " млс");

        Path resources = Paths.get("Lab1.GaussianBlur/src/main/resources").toAbsolutePath();
        File file = new File(resources + "/result.png");
        ImageIO.write(resultImage, "png", file);
    }

    public static int[][] convertToArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] pixelArray = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelArray[y][x] = image.getRGB(x, y);
            }
        }

        return pixelArray;
    }
}
