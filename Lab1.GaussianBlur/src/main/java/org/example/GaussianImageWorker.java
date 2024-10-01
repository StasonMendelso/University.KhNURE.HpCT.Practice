package org.example;

/**
 * @author Stanislav Hlova
 */
public class GaussianImageWorker extends Thread{

    private int[][] targetMatrix;
    private double[][] kernel;
    private int[][] strippedMatrix;
    private Point startPoint;
    private Point endPoint;
    private int startYPosition;

    public GaussianImageWorker(int[][] targetMatrix, double[][] kernel, int[][] strippedMatrix, Point startPoint, Point endPoint, int startYPosition) {
        this.targetMatrix = targetMatrix;
        this.kernel = kernel;
        this.strippedMatrix = strippedMatrix;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.startYPosition = startYPosition;
    }

    @Override
    public void run() {
       int[][] resultMatrix = GaussianBlurExecutor.blur(strippedMatrix, kernel, startPoint, endPoint);
       int strippedHeight = endPoint.getY() - startPoint.getY() + 1;

       for (int y = startYPosition, j = 0; j < strippedHeight; y++, j++){
           targetMatrix[y] = resultMatrix[j];
        }
    }
}
