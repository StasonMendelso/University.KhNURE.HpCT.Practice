package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.example.ArrayGeneratorUtil.generateRandomArray;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {

//        prewarmingJVM();
//        measuringOperationTimes();
//
//        double c = 1.79, m = 61.8265;
        int[] array = generateRandomArray(100000, 0,100000);

        long startTime = System.nanoTime(); // Початок заміру часу
        bubbleSort(array);
        long endTime = System.nanoTime();   // Кінець заміру часу

        System.out.println("Відсортований масив:");
        for (int num : array) {
            System.out.print(num + " ");
        }
        System.out.println("\nЧас виконання: " + (endTime - startTime) + " наносекунд");
      //  System.out.println("Теоретичний час виконання: " + calculateTheorOrderedBubleSort(array.length, c, m) + " наносекунд");


    }

    private static double calculateTheorOrderedBubleSort(int n, double c, double m) {
        return (c * n * (n - 1) / 2) + (m * n * (n - 1) / 4);
    }

    volatile static boolean condition = false;

    private static void measuringOperationTimes() {
        List<BigDecimal> resultComparing = new ArrayList<>();
        int arrayLength = 10;
        int[] array = new int[(int) arrayLength];
        for (int i = 0; i < 10; i++) {
            long operationCount = 100000000;
            long startTime = System.nanoTime();
            for (int j = 1; j <= operationCount; j++) {
                condition = j < j + 1;
            }
            long endTime = System.nanoTime();
            System.out.println("For " + operationCount + " of comparing take: " + (endTime - startTime) + "ns");
            BigDecimal result = BigDecimal.valueOf((endTime - startTime)).divide(BigDecimal.valueOf(operationCount));
            System.out.println("One operation of comparing takes: " + result + "ns");
            resultComparing.add(result);
        }
        System.out.println("Average result is " + resultComparing.stream().reduce(BigDecimal::add).get().divide(BigDecimal.valueOf(resultComparing.size())));

        List<BigDecimal> resultSwapping = new ArrayList<>();


        Arrays.fill(array, 1);
        for (int i = 0; i < 10; i++) {
            long operationCount = 0;
            long startTime = System.nanoTime();
            for (int k = 0; k < array.length - 1; k++) {
                for (int j = 0; j < array.length - 1 - k; j++) {
                    int temp = array[k];
                    array[k] = array[j + 1];
                    array[j + 1] = temp;
                    operationCount++;
                }
            }
            long endTime = System.nanoTime();
            System.out.println("For " + operationCount + " of swapping take: " + (endTime - startTime) + "ns");
            BigDecimal result = BigDecimal.valueOf((endTime - startTime)).divide(BigDecimal.valueOf(operationCount), RoundingMode.CEILING);
            System.out.println("One operation of swapping takes: " + result + "ns");
            resultSwapping.add(result);
        }
        System.out.println("Average result is " + resultSwapping.stream().reduce(BigDecimal::add).get().divide(BigDecimal.valueOf(resultSwapping.size())));
    }

    public static void bubbleSort(int[] array) {
        int n = array.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1 - i; j++) {
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }
    }

    private static void prewarmingJVM() {
        System.out.println("==== JVM Prewarming ====");
        for (int i = 0; i <= 10; i++) {
            long startTime = System.nanoTime();
            int sum = 1;
            for (int j = i; j < 100_000_000; j++) {
                sum = j * 2 + sum;
            }
            long endTime = System.nanoTime();
            System.out.println("Result of operation = " + sum + ". Operation of prewarming takes: " + (endTime - startTime) + "ns");
        }
        System.out.println("==== JVM was prewarmed ====");
    }

    // Вычисление количества элементов для каждого процесса
    private static int[] calculateCounts(int totalElements, int size) {
        int[] counts = new int[size];
        int quotient = totalElements / size;
        int remainder = totalElements % size;

        for (int i = 0; i < size; i++) {
            counts[i] = quotient + (i < remainder ? 1 : 0);
        }

        return counts;
    }

    // Вычисление смещений для каждого процесса
    private static int[] calculateDispls(int totalElements, int size) {
        int[] displs = new int[size];
        int[] counts = calculateCounts(totalElements, size);

        for (int i = 1; i < size; i++) {
            displs[i] = displs[i - 1] + counts[i - 1];
        }

        return displs;
    }


}
