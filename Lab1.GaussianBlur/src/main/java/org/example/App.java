package org.example;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;


public class App {
    public static final int KERNEL_SIZE = 23;
    public static final int SIGMA = 10;
    public static final List<CalculationResults> CALCULATION_RESULTS = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        int processors = Runtime.getRuntime().availableProcessors();
        System.out.println("Number of available processors = " + processors);
        System.out.println("Make a pre-warming of the JVM");
        String inputFilePath = "enot";
        String outputDirectoryPath = "enot_results";
        applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 1);
        applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 1);
        applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 1);
        applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 1);
        System.out.println("Finished a pre-warming of the JVM!");
        System.out.println("Start real computing!");
        CALCULATION_RESULTS.clear();
        int numberOfResearches = 10;
        for (int i = 1; i <= numberOfResearches; i++) {
            System.out.println("========================================EXPERIMENT №"+i+"========================================");
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 1);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 2);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 4);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 6);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 8);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 10);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 12);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 14);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 16);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 18);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 20);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 22);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 24);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 26);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 30);
            applyGaussianFilterToImage(inputFilePath, outputDirectoryPath, 40);
        }
        System.out.println("=================================================RESULTS======================================================");
        printCalculationResults();
    }

    private static void printCalculationResults() {
        Map<Integer, List<CalculationResults>> map = CALCULATION_RESULTS.stream()
                .collect(groupingBy(CalculationResults::getNumberOfThreads));

        StringBuilder tableHeader = new StringBuilder();
        tableHeader.append("|  p   |");
        map.keySet().stream().sorted().forEach(key -> tableHeader.append(String.format(" %10d |", key)));

        StringBuilder tableBody = new StringBuilder();
        tableBody.append("| t,мс |");
        map.keySet().stream().sorted().forEach(key -> {
            double avgTime = map.get(key).stream()
                    .collect(Collectors.averagingLong(CalculationResults::getTimeOfComputing));
            tableBody.append(String.format(" %10.1f |", avgTime));
        });

        System.out.println(tableHeader);
        System.out.println(tableBody);
    }

    private static void applyGaussianFilterToImage(String inputFilePath, String outputDirectoryPath, int numberOfThreads) throws IOException {
        InputStream inputStream = App.class.getClassLoader().getResourceAsStream(inputFilePath + ".png");
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        BufferedImage resultImage = applyGaussianFilter(bufferedImage, numberOfThreads);

        Path resources = Paths.get("Lab1.GaussianBlur/src/main/resources").toAbsolutePath();
        if (!Files.exists(resources.resolve(outputDirectoryPath))) {
            Files.createDirectory(resources.resolve(outputDirectoryPath));
        }
        File file = new File(resources + "/" + outputDirectoryPath + "/" + inputFilePath + "_" + numberOfThreads + ".png");
        ImageIO.write(resultImage, "png", file);
    }

    private static BufferedImage applyGaussianFilter(BufferedImage bufferedImage, int numberOfThreads) {
        int height = bufferedImage.getHeight();
        if (height < numberOfThreads) {
            throw new IllegalArgumentException("The image can't be handled by " + numberOfThreads + " threads, because the height of the image less than the number of threads. Height = " + height);
        }
        int width = bufferedImage.getWidth();
        int[][] pixels = convertToArray(bufferedImage);
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        long startTime = System.currentTimeMillis();
        double[][] gaussianKernel = GaussianBlurExecutor.createGaussianKernel(App.KERNEL_SIZE, App.SIGMA);

        int remainHeight = height, startY = 0, stripHeight = height / numberOfThreads, endY = startY + stripHeight - 1;
        int kernelHalf = gaussianKernel.length / 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<Future<?>> submittedTasks = new ArrayList<>();
        for (int threadNumber = 1; threadNumber <= numberOfThreads; threadNumber++) {
            int upperBuffer = Math.min(kernelHalf, startY);
            int lowerBuffer = Math.min(kernelHalf, height - endY - 1);

            int[][] strippedPixels = new int[stripHeight + upperBuffer + lowerBuffer][];
            int pointer = startY - upperBuffer;
            for (int index = 0; index < strippedPixels.length; index++, pointer++) {
                strippedPixels[index] = pixels[pointer];
            }

            Point startPoint = new Point(0, upperBuffer);
            Point endPoint = new Point(width - 1, upperBuffer + stripHeight - 1);

            int finalStartY = startY;
            submittedTasks.add(executorService.submit(() -> GaussianBlurExecutor.blur(strippedPixels, gaussianKernel, startPoint, endPoint, resultImage, finalStartY)));

            remainHeight -= stripHeight;
            if (remainHeight <= 2 * stripHeight && threadNumber + 1 == numberOfThreads) {
                stripHeight += remainHeight - stripHeight;
            }
            startY = endY + 1;
            endY = startY + stripHeight - 1;
        }
        waitAllSubmittedTasks(submittedTasks);
        long endTime = System.currentTimeMillis();
        long timeOfComputing = endTime - startTime;
        System.out.printf("Розмір зображення %dx%d. Кількість потоків = %d. Час виконання = %d млс%n", width, height, numberOfThreads, timeOfComputing);
        CALCULATION_RESULTS.add(new CalculationResults(width, height, numberOfThreads, timeOfComputing));
        executorService.shutdown();
        return resultImage;
    }

    private static void waitAllSubmittedTasks(List<Future<?>> submittedTasks) {
        submittedTasks.forEach(task -> {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Task can't be finished!");
                throw new RuntimeException(e);
            }
        });
    }

    private static int[][] convertToArray(BufferedImage image) {
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
