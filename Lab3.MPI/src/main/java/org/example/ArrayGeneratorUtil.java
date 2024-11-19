package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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

    public static void main(String[] args) throws IOException {
        int n = 100_001;
        int[] randomArray = generateRandomArray(n, 1, 100_000);
        Files.writeString(Path.of("Lab3.MPI/src/main/resources/array.txt"), String.join(",", Arrays.stream(randomArray).mapToObj(String::valueOf).toList()));
    }
}
