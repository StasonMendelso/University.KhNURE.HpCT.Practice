package org.example;

/**
 * @author Stanislav Hlova
 */
public class CalculationResults {
    private int width;
    private int height;
    private int numberOfThreads;
    private long timeOfComputing;

    public CalculationResults(int width, int height, int numberOfThreads, long timeOfComputing) {
        this.width = width;
        this.height = height;
        this.numberOfThreads = numberOfThreads;
        this.timeOfComputing = timeOfComputing;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public long getTimeOfComputing() {
        return timeOfComputing;
    }
}
