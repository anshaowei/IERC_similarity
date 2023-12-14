package net.csibio.metaphoenix.client.utils;

public class ArrayUtil {

    public static double  sum(double[] array) {
        double sum = 0;
        for (double d : array) {
            sum += d;
        }
        return sum;
    }

    public static double[] normalize(double[] array, double value) {
        if (value > 0) {
            for (int i = 0; i < array.length; i++) {
                array[i] /= value;
            }
        }
        return array;
    }

    public static double[] Ionnormalize(double[] array, double value) {
        if (value > 0) {
            for (int i = 0; i < array.length; i++) {
                array[i] = array[i] / value * 100;
            }
        }
        return array;
    }

    public static int findNearestIndex(double[] mzs, double mz) {
        int left = 0, right = 0;
        for (right = mzs.length - 1; left != right; ) {
            int midIndex = (right + left) / 2;
            int mid = (right - left);
            double midValue = mzs[midIndex];
            if (mz == midValue) {
                return midIndex;
            }
            if (mz > midValue) {
                left = midIndex;
            } else {
                right = midIndex;
            }
            if (mid <= 2) {
                break;
            }
        }
        return (Math.abs(mzs[right] - mz) > Math.abs(mzs[left] - mz) ? left : right);
    }

    public static double findNearestDiff(double[] mzs, double mz) {
        return Math.abs(mzs[findNearestIndex(mzs, mz)] - mz);
    }
}
