
#include "omp.h"
#include <chrono>
#include <iostream>
#include <fstream>
#include <sstream>
#include <vector> 
#include <string> 
using namespace std;
void writeToCSV(const string&, const vector<vector<string>>&);
double findSequentialAverageTime(int** matrix, int size, int count);
double findParallelAverageTime(int** matrix, int size, int threadNumber, int count);
double findParallelWithReductionAverageTime(int** matrix, int size, int threadNumber, int count);

void printArray(int** matrix, int n) {
    cout << "Array is:\n";
    cout << "[\n";
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            cout << matrix[i][j] << ", ";
        }
        cout << "\n";
    }
    cout << "]\n";
}

int sequental(int** matrix, int n) {
    int max = 0;
    for (int i = 0; i < n; i++) {
        int localSum = 0;
        for (int j = 0; j < n; j++) {
            localSum += matrix[i][j];
        }
        if (localSum > max) {
            max = localSum;
        }
    }

    return max;
}
int parallel(int** matrix, int n, int threadNumber) {
    int maxRowSum = 0; 
    #pragma omp parallel num_threads(threadNumber)
    {
        int localMax = 0;
        #pragma omp for
        for (int i = 0; i < n; ++i) {
            int rowSum = 0;
            for (int j = 0; j < n; ++j) {
                rowSum += matrix[i][j];
            }
            if (rowSum > localMax) {
                localMax = rowSum;
            }
        }

        #pragma omp critical
        {
            if (localMax > maxRowSum) {
                maxRowSum = localMax;
            }
        }
    }

    return maxRowSum;
}
int parallelWithReduction(int** matrix, int n, int threadNumber) {
    int maxRowSum = 0;

    #pragma omp parallel for num_threads(threadNumber) reduction(max: maxRowSum)
    for (int i = 0; i < n; ++i) {
        int rowSum = 0;
        for (int j = 0; j < n; ++j) {
            rowSum += matrix[i][j];
        }
        if (rowSum > maxRowSum) {
            maxRowSum = rowSum;
        }
    }

    return maxRowSum;
}


void deleteMatrix(int** matrix, int n) {
    for (int i = 0; i < n; i++) {
        delete matrix[i];
    }
    delete matrix;
}


int main()
{
    const int numberOfThreads = 50;
    const int n = 100;
    int** matrix = new int*[n];
    int d = 1;
    for (int i = 0; i < n; i++) {
        matrix[i] = new int[n];
        for (int j = 0; j < n; j++){
            matrix[i][j] = d++;
        }
    }

    //printArray(matrix, n);
    printf("Size of matrix is %d\nNumber of threads is %d\n", n, numberOfThreads);

    double startOpenMP = omp_get_wtime();
    int max = sequental(matrix, n);
    double endOpenMP = omp_get_wtime();
    auto duration = (endOpenMP - startOpenMP);
    printf("Sequential. Max sum of row is %d. Sequential time is %.9f seconds (%.0f nanoseconds)\n", max, duration, duration * 1e9);

    startOpenMP = omp_get_wtime();
    max = parallel(matrix, n, numberOfThreads);
    endOpenMP = omp_get_wtime();
    double durationMP = (endOpenMP - startOpenMP);
    printf("Parallel. Max sum of row is %d. Parallel time is %.9f seconds (%.0f nanoseconds)\n", max, durationMP, durationMP * 1e9);

    startOpenMP = omp_get_wtime();
    max = parallelWithReduction(matrix, n, numberOfThreads);
    endOpenMP = omp_get_wtime();
    double durationMPReduction = (endOpenMP - startOpenMP);
    printf("ParallelReduction. Max sum of row is %d. ParallelReduction time is %.9f seconds (%.0f nanoseconds)\n", max, durationMPReduction, durationMPReduction * 1e9);

    deleteMatrix(matrix, n);
    vector<vector<string>> data;
    vector<string> headers = { "n", "T1", "T2", "T4", "T6", "T8", "T10", "T12", "T14", "T16","T18", "T20", "T2(red)", "T4(red)", "T6(red)", "T8(red)", "T10(red)", "T12(red)","T14(red)", "T16(red)", "T18(red)", "T20(red)" };
    data.push_back(headers);
    int maxSizeOfMatrix = 2500;
    for (int i = 1; i <= maxSizeOfMatrix; i++){
        cout << "Computing for matrix size: " << i << endl;
        int** matrix = new int* [i];
        int d = 1;
        for (int k = 0; k < i; k++) {
            matrix[k] = new int[i];
            for (int j = 0; j < i; j++) {
                matrix[k][j] = d++;
            }
        }
        vector<string> result = {};
        result.push_back(to_string(i));
        int count = 5;
        double averTime = findSequentialAverageTime(matrix, i, count);
        ostringstream oss;
        oss.precision(9); 
        oss << std::fixed << averTime;
        result.push_back(oss.str());
        for (int p = 2; p <= 20; p+=2) {
            averTime = findParallelAverageTime(matrix, i, p, count);
            ostringstream oss;
            oss.precision(9);
            oss << std::fixed << averTime;
            result.push_back(oss.str());
        }
        for (int p = 2; p <= 20; p+=2){
            averTime = findParallelWithReductionAverageTime(matrix, i, p, count);
            ostringstream oss;
            oss.precision(9);
            oss << std::fixed << averTime;
            result.push_back(oss.str());
        }
        deleteMatrix(matrix, i);
        data.push_back(result);
    }

    writeToCSV("results.csv", data);
    return 0;
}


double findSequentialAverageTime(int **matrix, int size, int count) {
    double sum = 0;
    for (int i = 0; i < count; i++)
    {
        double startOpenMP = omp_get_wtime();
        int max = sequental(matrix, size);
        double endOpenMP = omp_get_wtime();
        sum+= (endOpenMP - startOpenMP);
    }
    return sum / (double)count;
}

double findParallelAverageTime(int **matrix, int size, int threadNumber, int count) {
    double sum = 0;
    for (int i = 0; i < count; i++)
    {
        double startOpenMP = omp_get_wtime();
        int max = parallel(matrix, size, threadNumber);
        double endOpenMP = omp_get_wtime();
        sum+= (endOpenMP - startOpenMP);
    }
    return sum / (double)count;
}
double findParallelWithReductionAverageTime(int **matrix, int size, int threadNumber, int count) {
    double sum = 0;
    for (int i = 0; i < count; i++)
    {
        double startOpenMP = omp_get_wtime();
        int max = parallelWithReduction(matrix, size, threadNumber);
        double endOpenMP = omp_get_wtime();
        sum+= (endOpenMP - startOpenMP);
    }
    return sum / (double)count;
}

void writeToCSV(const string& filename, const vector<vector<string>>& data) {
    ofstream file(filename);

    if (!file.is_open()) {
        cerr << "Error: Could not open file " << filename << endl;
        return;
    }

    for (const auto& row : data) {
        for (size_t i = 0; i < row.size(); ++i) {
            file << row[i];
            if (i < row.size() - 1) {
                file << ";"; 
            }
        }
        file << "\n"; 
    }

    file.close();
    cout << "Data successfully written to " << filename << endl;
}