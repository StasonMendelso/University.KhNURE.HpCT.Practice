package org.example;


import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Stanislav Hlova
 */
public class GaussianBlurExecutor {
    public static double[][] createGaussianKernel(int kernelSize, double sigma) {
        double[][] kernel = new double[kernelSize][kernelSize];

        int indent = kernelSize / 2;

        double sum = 0.0;
        for (int y = -indent; y <= indent; y++) {
            for (int x = -indent; x <= indent; x++) {
                double value = gaussian(x, y, sigma);
                sum += value;
                kernel[y + indent][x + indent] = value;
            }
        }

        for (double[] row : kernel) {
            for (int x = 0; x < kernelSize; x++) {
                row[x] = row[x] / sum;
            }
        }

        return kernel;
    }

    private static double gaussian(int x, int y, double sigma) {
        return (1 / (2 * Math.PI * Math.pow(sigma, 2))) * Math.exp(-((x * x + y * y) / (2 * Math.pow(sigma, 2))));
    }

    public static void blur(int[][] matrix, double[][] kernel, Point point1, Point point2, BufferedImage resultImage, int startYPosition) {
        if (point2.getY() < point1.getY()) {
            throw new IllegalArgumentException("Point2.Y can't be less than Point1.Y");
        }
        if (point2.getX() < point1.getX()) {
            throw new IllegalArgumentException("Point2.Y can't be less than Point1.Y");
        }
        int kernelSize = kernel.length;
        int kernelHalf = kernelSize / 2;

        for (int y = point1.getY(); y <= point2.getY(); y++, startYPosition++) {
            for (int x = point1.getX(); x <= point2.getX(); x++) {
                double sumR = 0, sumG = 0, sumB = 0;

                for (int ky = 0; ky < kernelSize; ky++) {
                    for (int kx = 0; kx < kernelSize; kx++) {
                        int pixelX = x - kernelHalf + kx;
                        int pixelY = y - kernelHalf + ky;

                        if (pixelX < 0) {
                            pixelX = -pixelX;
                        } else if (pixelX >= matrix[0].length) {
                            pixelX = 2 * matrix[0].length - pixelX - 1;
                        }

                        if (pixelY < 0) {
                            pixelY = -pixelY;
                        } else if (pixelY >= matrix.length) {
                            pixelY = 2 * matrix.length - pixelY - 1;
                        }

                        int rgb = matrix[pixelY][pixelX];

                        Color color = new Color(rgb);

                        sumR += color.getRed() * kernel[ky][kx];
                        sumG += color.getGreen() * kernel[ky][kx];
                        sumB += color.getBlue() * kernel[ky][kx];
                    }
                }

                int newRed = Math.min(Math.max((int) sumR, 0), 255);
                int newGreen = Math.min(Math.max((int) sumG, 0), 255);
                int newBlue = Math.min(Math.max((int) sumB, 0), 255);

                Color color = new Color(newRed, newGreen, newBlue);
                resultImage.setRGB(x,startYPosition, color.getRGB());
            }
        }
    }
}
