package org.example;

/**
 * Hello world!
 */
public class App {
    //    public static void main(String[] args) {
//        int m = 30;
//        for (int n = 2; n <= m; n++) {
//            int sumReverse = 0;
//            for (int i = 1; i <= n; i++) {
//                sumReverse += 2 * i - 1;
//            }
//            int sumForward = 0;
//            for (int i = 1; i <= n - 1; i++) {
//                sumForward += 2 * Math.pow(n - i, 2) + 4 * (n - i);
//            }
//            System.out.printf("For n = %d, sumForward = %d, sumReverse = %d, sum = %d %n", n, sumForward, sumReverse, sumForward + sumReverse);
//        }
//    }
//    public static void main(String[] args) {
//        int m = 30;
//        int p = 8;
//        for (int n = 2; n <= m; n++) {
//            if (n<p){continue;}
//            double sum1 = 0;
//            for (int i = 1; i <= p; i++) {
//                double sum2 = 0;
//                for (int j = ((int) Math.ceil((double) n / p)) * (i - 1) + 1; j <= i * ((int) Math.ceil((double) n / p)); j++) {
//                    sum2 += (n - i + 2);
//                }
//                sum1 += sum2;
//            }
//            sum1 *= (double) (2 * n) / p;
//            sum1 += 3 * n;
//            System.out.printf("For n = %d, p=%d, Tp = %f %n", n, p, sum1);
//        }
//    }
    public static void main(String[] args) {
        double n = 50;
        double p = 5;
        double t = 0.001;
        double a = 10;
        double b = 100;
        double d = 8;
        double sum = 3 * n * t + ((a + d / b) * (n - 1));

        double sum1 = 0;
        for (int i = 1; i <= p; i++) {
            double sum2 = 0;
            for (int j = ((int) Math.ceil(n / p)) * (i - 1) + 1; j <= i * ((int) Math.ceil((double) n / p)); j++) {
                sum2 += (n - i + 2) * 2 * n * t / p + a + n * d / b;
            }
            sum1 += sum2;
        }
        sum += sum1;

        System.out.println(sum);
    }
}
