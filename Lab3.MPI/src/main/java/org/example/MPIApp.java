package org.example;

import mpi.Cartcomm;
import mpi.MPI;
import mpi.ShiftParms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.example.ArrayGeneratorUtil.generateRandomArray;

/**
 * @author Stanislav Hlova
 */
public class MPIApp {
    private static final boolean DEBUG_DISABLED = true;
    public static final String PATH_TO_DEBUG_FOLDER = "D:\\ХНУРЕ\\7 семестр\\ТВО\\University.KhNURE.HpCT.Practice\\Lab3.MPI\\debug\\";

    public static void main(String[] args) throws IOException {
        //  int[] initialArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] initialArray = Arrays.stream(Files.readString(Path.of("D:\\ХНУРЕ\\7 семестр\\ТВО\\University.KhNURE.HpCT.Practice\\Lab3.MPI\\src\\main\\resources\\array.txt"))
                        .split(","))
                        .mapToInt(Integer::parseInt)
                        .toArray();
        int[] finalArray = null;
        long startTime = System.nanoTime();
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank(); // Ранг процесса
        int size = MPI.COMM_WORLD.Size(); // Общее количество процессов
        int sizeOfSubArray = initialArray.length / size;
        int[] mySubProcessArray = new int[sizeOfSubArray];
        if (!DEBUG_DISABLED) {
            clearLogFile(rank);
        }
        if (size * sizeOfSubArray != initialArray.length) {
            throw new RuntimeException("Кількість елементів в масиві повинна націло ділитися на кількість процесів без остачі!");
        }
        if (size % 2 == 0 || size == 1){
            throw new RuntimeException("Кількість процесів повинна бути непарною та більше за 1!");
        }

        //Створення топології лінійки
        int[] dims = {size}; // Оскільки в нас лінійка, це одновимірний масив
        boolean[] periods = {false}; // Без періодичності (не по колу)
        Cartcomm lineComm = MPI.COMM_WORLD.Create_cart(dims, periods, false);

        debug(rank, "=== Процеc відправки даних ===");
        if (isRoot(rank, size)) {
            System.out.println("Початковий масив: " + Arrays.toString(initialArray));
            int numberOfSends = size / 2;
            //Розсилка ліворуч
            int k = 0;
            for (int i = 0; i < numberOfSends; i++, k++) {
                debug(rank, "Відправка даних ліворуч з РУТА на " + (rank - 1));
                int[] subArrayToSend = Arrays.copyOfRange(initialArray, k * sizeOfSubArray, (k + 1) * sizeOfSubArray);
                lineComm.Send(subArrayToSend, 0, sizeOfSubArray, MPI.INT, rank - 1, 0);
            }
            //Середній залишаємо собі
            mySubProcessArray = Arrays.copyOfRange(initialArray, k * sizeOfSubArray, (k + 1) * sizeOfSubArray);
            k = size - 1;
            // Розсилка праворуч
            for (int i = 0; i < numberOfSends; i++, k--) {
                debug(rank, "Відправка даних праворуч з РУТА на " + (rank + 1));
                int[] subArrayToSend = Arrays.copyOfRange(initialArray, k * sizeOfSubArray, (k + 1) * sizeOfSubArray);
                lineComm.Send(subArrayToSend, 0, sizeOfSubArray, MPI.INT, rank + 1, 0);
            }

        }

        ShiftParms shiftParms = null;

        if (rank < size / 2) {
            //Розсилка ліворуч
            shiftParms = lineComm.Shift(0, -1);
            int sendCountLeft = size / 2 - Math.abs(size / 2 - rank);
            int recCountFromRight = size / 2 - Math.abs(size / 2 - rank) + 1;
            while (sendCountLeft != 0 || recCountFromRight != 0) {
                if (recCountFromRight != 0) {
                    debug(rank, "Отримання даних процесом " + rank + " ліворуч з " + shiftParms.rank_source);
                    lineComm.Recv(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_source, 0);
                    recCountFromRight--;
                }
                if (sendCountLeft != 0) {
                    debug(rank, "Відправка даних процесом " + rank + " ліворуч на " + shiftParms.rank_dest);
                    lineComm.Send(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_dest, 0);
                    sendCountLeft--;
                }
            }
        } else if (rank > size / 2) {
            //Розсилка праворуч
            shiftParms = lineComm.Shift(0, 1);
            int sendCountRight = size / 2 - Math.abs(size / 2 - rank);
            int recCountFromLeft = size / 2 - Math.abs(size / 2 - rank) + 1;
            while (sendCountRight != 0 || recCountFromLeft != 0) {
                if (recCountFromLeft != 0) {
                    debug(rank, "Отримання даних процесом " + rank + " праворуч з " + shiftParms.rank_source);
                    lineComm.Recv(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_source, 0);
                    recCountFromLeft--;
                }
                if (sendCountRight != 0) {
                    debug(rank, "Відправка даних процесом " + rank + " праворуч на " + shiftParms.rank_dest);
                    lineComm.Send(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_dest, 0);
                    sendCountRight--;
                }
            }
        }
        debug(rank, "Процес " + rank + " в решті решт отримав масив " + Arrays.toString(mySubProcessArray));
        debug(rank, "=== Процес сортування ===");
        Arrays.sort(mySubProcessArray);
        int[] receivedSubProcessArray = new int[sizeOfSubArray];
        for (int i = 1; i <= size; i++) {
            if (i % 2 == 1) {
                debug(rank, "== Непарна ітерацій №" + i + " ==");
                if (rank % 2 == 0) {
                    if (rank != size - 1) {
                        int neighbourRank = rank + 1;
                        debug(rank, "Відправляємо праворуч на непарного сусіда " + neighbourRank + " свій масив " + Arrays.toString(mySubProcessArray));
                        lineComm.Send(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);
                        debug(rank, "Отримуємо з правого непарного сусіда " + neighbourRank + " його масив ");
                        lineComm.Recv(receivedSubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);
                        debug(rank, "Отримали з правого непарного сусіда " + neighbourRank + " його масив " + Arrays.toString(receivedSubProcessArray));
                        debug(rank, "Сортуємо масиви методом злиття та залишаючи собі менші значення. Мій масив " + Arrays.toString(mySubProcessArray) + ", масив сусіда " + Arrays.toString(receivedSubProcessArray));
                        mySubProcessArray = mergeLower(mySubProcessArray, receivedSubProcessArray, sizeOfSubArray);
                        debug(rank, "Залишаємо менші значення собі: " + Arrays.toString(mySubProcessArray));
                    }
                } else {
                    int neighbourRank = rank - 1;
                    debug(rank, "Отримуємо з лівого парного сусіда " + neighbourRank + " його масив ");
                    lineComm.Recv(receivedSubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);
                    debug(rank, "Отримали з лівого парного сусіда " + neighbourRank + " його масив " + Arrays.toString(receivedSubProcessArray));
                    debug(rank, "Відправляємо ліворуч на парного сусіда " + neighbourRank + " свій масив " + Arrays.toString(mySubProcessArray));
                    lineComm.Send(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);

                    debug(rank, "Сортуємо масиви методом злиття та залишаючи собі більші значення. Мій масив " + Arrays.toString(mySubProcessArray) + ", масив сусіда " + Arrays.toString(receivedSubProcessArray));
                    mySubProcessArray = mergeHigher(mySubProcessArray, receivedSubProcessArray, sizeOfSubArray);
                    debug(rank, "Залишаємо більші значення собі: " + Arrays.toString(mySubProcessArray));
                }
            } else {
                debug(rank, "== Парна ітерацій №" + i + " ==");
                if (rank % 2 != 0) {
                    int neighbourRank = rank + 1;
                    debug(rank, "Відправляємо праворуч на парного сусіда " + neighbourRank + " свій масив " + Arrays.toString(mySubProcessArray));
                    lineComm.Send(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);
                    debug(rank, "Отримуємо з правого парного сусіда " + neighbourRank + " його масив ");
                    lineComm.Recv(receivedSubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);
                    debug(rank, "Отримали з правого парного сусіда " + neighbourRank + " його масив " + Arrays.toString(receivedSubProcessArray));
                    debug(rank, "Сортуємо масиви методом злиття та залишаючи собі менші значення. Мій масив " + Arrays.toString(mySubProcessArray) + ", масив сусіда " + Arrays.toString(receivedSubProcessArray));
                    mySubProcessArray = mergeLower(mySubProcessArray, receivedSubProcessArray, sizeOfSubArray);
                    debug(rank, "Залишаємо менші значення собі: " + Arrays.toString(mySubProcessArray));
                } else {
                    if (rank != 0) {
                        int neighbourRank = rank - 1;
                        debug(rank, "Отримуємо з лівого непарного сусіда " + neighbourRank + " його масив ");
                        lineComm.Recv(receivedSubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);
                        debug(rank, "Отримали з лівого парного сусіда " + neighbourRank + " його масив " + Arrays.toString(receivedSubProcessArray));
                        debug(rank, "Відправляємо ліворуч на непарного сусіда " + neighbourRank + " свій масив " + Arrays.toString(mySubProcessArray));
                        lineComm.Send(mySubProcessArray, 0, sizeOfSubArray, MPI.INT, neighbourRank, 0);

                        debug(rank, "Сортуємо масиви методом злиття та залишаючи собі більші значення. Мій масив " + Arrays.toString(mySubProcessArray) + ", масив сусіда " + Arrays.toString(receivedSubProcessArray));
                        mySubProcessArray = mergeHigher(mySubProcessArray, receivedSubProcessArray, sizeOfSubArray);
                        debug(rank, "Залишаємо більші значення собі: " + Arrays.toString(mySubProcessArray));
                    }

                }
            }
        }
        debug(rank, "Процес " + rank + " має наступну відсортовану частину: " + Arrays.toString(mySubProcessArray));
        debug(rank, "===Збір даних====");

        if (isRoot(rank, size)) {
            finalArray = new int[initialArray.length];
            int index = 0;
            int numberOfSends = size / 2;
            //Збір ліворуч
            int k = 0;
            for (int i = 0; i < numberOfSends; i++, k++) {
                debug(rank, "Збір даних ліворуч на РУТ з " + k + " через сусіда " + (rank - 1) + ". ОЧікуємо повідомлення з тегом " + k);
                int[] subArrayReceived = new int[sizeOfSubArray];
                //Отримуємо масив з лівої частини топології з сусіднього процеса, але за тегом, щоб у правильному порядку побудувати вихідний масив
                lineComm.Recv(subArrayReceived, 0, sizeOfSubArray, MPI.INT, rank - 1, k);
                debug(rank, "Отримуємо повідомлення " + Arrays.toString(subArrayReceived) + " з тегом " + k);
                for (int value : subArrayReceived) {
                    finalArray[index++] = value;
                }
            }
            for (int value : mySubProcessArray) {
                finalArray[index++] = value;
            }
            k = size - 1;
            index = finalArray.length - 1;
            // Збір праворуч
            for (int i = 0; i < numberOfSends; i++, k--) {
                debug(rank, "Збір даних праворуч на РУТ з " + k + " через сусіда " + (rank + 1) + ". Очікуємо повідомлення з тегом " + k);
                int[] subArrayReceived = new int[sizeOfSubArray];
                //Отримуємо масив з правої частини топології з сусіднього процеса, але за тегом, щоб у правильному порядку побудувати вихідний масив
                lineComm.Recv(subArrayReceived, 0, sizeOfSubArray, MPI.INT, rank + 1, k);
                debug(rank, "Отримуємо повідомлення " + Arrays.toString(subArrayReceived) + " з тегом " + k);
                for (int j = subArrayReceived.length - 1; j >= 0; j--) {
                    finalArray[index--] = subArrayReceived[j];
                }
            }
        }
        // На збір даних
        if (rank < size / 2) {
            shiftParms = lineComm.Shift(0, 1);
            int sendCountLeft = size / 2 - Math.abs(size / 2 - rank) + 1;
            int recCountFromRight = size / 2 - Math.abs(size / 2 - rank);
            int k = 0;
            int[] bufArray = new int[sizeOfSubArray];
            while (sendCountLeft != 0 || recCountFromRight != 0) {
                if (recCountFromRight != 0) {
                    debug(rank, "Отримання даних процесом " + rank + " ліворуч з " + shiftParms.rank_source + " з тегом " + k);
                    lineComm.Recv(bufArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_source, k);
                    debug(rank, "Отримуємо повідомлення " + Arrays.toString(bufArray) + " з тегом " + k);
                    recCountFromRight--;
                }
                if (sendCountLeft != 0) {
                    if (sendCountLeft == 1) {
                        k = rank;
                        bufArray = mySubProcessArray;
                    }
                    debug(rank, "Відправка даних процесом " + rank + " ліворуч на РУТ на " + shiftParms.rank_dest + " повідомлення " + Arrays.toString(bufArray) + " з тегом " + k);
                    lineComm.Send(bufArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_dest, k);
                    sendCountLeft--;
                    k++;
                }
            }
        } else if (rank > size / 2) {
            shiftParms = lineComm.Shift(0, -1);
            int sendCountLeft = size / 2 - Math.abs(size / 2 - rank) + 1;
            int recCountFromRight = size / 2 - Math.abs(size / 2 - rank);
            int k = size - 1;
            int[] bufArray = new int[sizeOfSubArray];
            while (sendCountLeft != 0 || recCountFromRight != 0) {
                if (recCountFromRight != 0) {
                    debug(rank, "Отримання даних процесом " + rank + " праворуч з " + shiftParms.rank_source);
                    lineComm.Recv(bufArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_source, k);
                    recCountFromRight--;
                }
                if (sendCountLeft != 0) {
                    if (sendCountLeft == 1) {
                        k = rank;
                        bufArray = mySubProcessArray;
                    }
                    debug(rank, "Відправка даних процесом " + rank + " праворуч на РУТ на " + shiftParms.rank_dest + " з тегом " + k);
                    lineComm.Send(bufArray, 0, sizeOfSubArray, MPI.INT, shiftParms.rank_dest, k);
                    sendCountLeft--;
                    k--;
                }
            }
        }
        if (isRoot(rank, size)) {
            long endTime = System.nanoTime();
            System.out.println("Після отримання всіх повідомлень маємо наступний результат:");
            System.out.println(Arrays.toString(finalArray));
            debug(rank, "Після отримання всіх повідомлень маємо наступний результат:");
            debug(rank, Arrays.toString(finalArray));
            System.out.println("Час сортування за допомогою MPI дорівнює " + (endTime - startTime) + "нс");
        }
        MPI.Finalize();

    }

    private static int[] mergeLower(int[] left, int[] right, int sizeOfSubArray) {
        int[] result = new int[sizeOfSubArray];
        int i = 0, j = 0, k = 0;
        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                result[k++] = left[i++];
            } else {
                result[k++] = right[j++];
            }
            if (k == result.length) {
                return result;
            }
        }
        while (i < left.length) {
            result[k++] = left[i++];
            if (k == result.length) {
                return result;
            }
        }

        while (j < right.length) {
            result[k++] = right[j++];
            if (k == result.length) {
                return result;
            }
        }
        return result;
    }

    private static int[] mergeHigher(int[] left, int[] right, int sizeOfSubArray) {
        int[] result = new int[sizeOfSubArray];
        int i = left.length - 1, j = right.length - 1, k = sizeOfSubArray - 1;
        while (i > -1 && j > -1) {
            if (left[i] >= right[j]) {
                result[k--] = left[i--];
            } else {
                result[k--] = right[j--];
            }
            if (k == -1) {
                return result;
            }
        }
        while (i > -1) {
            result[k--] = left[i--];
            if (k == -1) {
                return result;
            }
        }

        while (j > -1) {
            result[k--] = right[j--];
            if (k == -1) {
                return result;
            }
        }
        return result;
    }

    private static void clearLogFile(int rank) {
        try {
            Path path = Path.of(PATH_TO_DEBUG_FOLDER + "rank" + rank + ".txt");
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isRoot(int rank, int size) {
        return rank == size / 2;
    }

    private static void debug(int rank, String msg) {
        if (DEBUG_DISABLED) {
            return;
        }
        try {
            Path path = Path.of(PATH_TO_DEBUG_FOLDER + "rank" + rank + ".txt");
            String message = String.format("[%-29s] - %s%n", LocalDateTime.now(), msg);
            if (Files.exists(path)) {
                Files.writeString(path, message, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, message, StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
