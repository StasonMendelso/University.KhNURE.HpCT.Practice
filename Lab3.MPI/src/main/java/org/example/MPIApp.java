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

/**
 * @author Stanislav Hlova
 */
public class MPIApp {
    private static final boolean DEBUG_DISABLED = false;

    public static void main(String[] args) {
        int[] initialArray = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        int[] finalArray = null;
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank(); // Ранг процесса
        int size = MPI.COMM_WORLD.Size(); // Общее количество процессов
        int sizeOfSubArray = initialArray.length / size;
        int[] mySubProcessArray = new int[sizeOfSubArray];
        if (!DEBUG_DISABLED) {
            clearLogFile(rank);
        }

        //Створення топології лінійки
        int[] dims = {size}; // Оскільки в нас лінійка, це одновимірний масив
        boolean[] periods = {false}; // Без періодичності (не по колу)
        Cartcomm lineComm = MPI.COMM_WORLD.Create_cart(dims, periods, false);

        debug(rank, "=== Процеc відправки даних ===");
        if (isRoot(rank, size)) {
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
        // На розсилку даних не з рутового

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

        debug(rank, "===Збір даних====");

        lineComm.Barrier();
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
                }
            }
        }
        if (isRoot(rank, size)) {
            System.out.println("Після отримання всіх повідомлень маємо наступний результат:");
            System.out.println(Arrays.toString(finalArray));
            debug(rank, "Після отримання всіх повідомлень маємо наступний результат:");
            debug(rank, Arrays.toString(finalArray));
        }
        MPI.Finalize();
    }

    private static void clearLogFile(int rank) {
        try {
            Path path = Path.of("D:\\ХНУРЕ\\7 семестр\\ТВО\\University.KhNURE.HpCT.Practice\\Lab3.MPI\\debug\\" + "rank" + rank + ".txt");
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
            Path path = Path.of("D:\\ХНУРЕ\\7 семестр\\ТВО\\University.KhNURE.HpCT.Practice\\Lab3.MPI\\debug\\" + "rank" + rank + ".txt");
            String message = "[" + LocalDateTime.now() + "] - " + msg + System.lineSeparator();
            if (Files.exists(path)) {
                Files.writeString(path, message, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, message, StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // Слияние двух массивов
    private static int[] merge(int[] left, int[] right) {
        int[] result = new int[left.length + right.length];
        int i = 0, j = 0, k = 0;

        while (i < left.length && j < right.length) {
            if (left[i] <= right[j]) {
                result[k++] = left[i++];
            } else {
                result[k++] = right[j++];
            }
        }

        while (i < left.length) {
            result[k++] = left[i++];
        }

        while (j < right.length) {
            result[k++] = right[j++];
        }

        return result;
    }
}
