#include <iostream>
#include <vector>

using namespace std;

void printVector(const vector<int>& arr) {
    cout << "[";
    for (size_t i = 0; i < arr.size(); ++i) {
        cout << arr[i];
        if (i < arr.size() - 1) cout << ", ";
    }
    cout << "]" << endl;
}

void quickSort(vector<int>& arr, int left, int right, int depth = 1) {
    if (left >= right) return;

    int pivot = arr[right];
    cout << "\nLuot " << depth << ":\n";
    cout << "Pivot = " << pivot << endl;

    int i = left - 1;
    for (int j = left; j < right; ++j) {
        if (arr[j] < pivot) {
            ++i;
            if (i != j) {
                cout << "Hoan vi " << arr[i] << " voi " << arr[j] << " o vi tri " << i << " va " << j << ":\n";
                swap(arr[i], arr[j]);
                printVector(arr);
            }
        }
    }

    ++i;
    if (i != right) {
        cout << "Hoan vi " << arr[i] << " voi " << arr[right] << " o vi tri " << i << " va " << right << ":\n";
        swap(arr[i], arr[right]);
        printVector(arr);
    }

    cout << "\nDoan 1:\n[";
    for (int k = left; k < i; ++k) {
        cout << arr[k];
        if (k < i - 1) cout << ", ";
    }
    cout << "]" << endl;

    cout << "Doan 2:\n[";
    for (int k = i + 1; k <= right; ++k) {
        cout << arr[k];
        if (k < right) cout << ", ";
    }
    cout << "]" << endl;

    quickSort(arr, left, i - 1, depth + 1);
    quickSort(arr, i + 1, right, depth + 1);
}

//int main() {
//int temp[] = {12, 5, 8, 23, 9, 11, 67, 34, 45, 54, 38, 41};
//vector<int> arr(temp, temp + sizeof(temp) / sizeof(temp[0]));
//    cout << "\nDanh sach ban dau:\n";
//    printVector(arr);
//
//    cout << "\nBat dau sap xep...\n";
//    quickSort(arr, 0, arr.size() - 1);
//
//    cout << "\nDanh sach da sap xep:\n";
//    printVector(arr);
//
//    return 0;
//}

