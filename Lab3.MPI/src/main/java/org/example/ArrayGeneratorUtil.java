package org.example;

import java.util.Random;

/**
 * @author Stanislav Hlova
 */
public class ArrayGeneratorUtil {
    public static int[] generateRandomArray(int n, int min, int max) {
        Random random = new Random();
        int[] array = new int[n];
        for (int i = 0; i < n; i++) {
            array[i] = random.nextInt(max - min + 1) + min; // Генерація числа в діапазоні [min, max]
        }
        return array;
    }
}
