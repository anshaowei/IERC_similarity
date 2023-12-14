package net.csibio.metaphoenix.client.algorithm.entropy;

import net.csibio.aird.bean.common.Spectrum;

public class Entropy {

    public static double getEntropy(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        double entropy = 0;
        for (double value : values) {
            double p = value / sum;
            if (p != 0) {
                entropy += p * Math.log(p);
            }

        }
        if (entropy == 0.0) {
            return entropy;
        }
        return -entropy;  //这里的entropy返回的是公式表中的 S'
    }

    public static double getIonEntropy(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        double entropy = 0d;
        for (double value : values) {
            double p = value / sum;
            entropy += p * Math.log(p);
        }
        entropy = entropy / Math.log(values.length);

        return -entropy;  //这里的entropy返回的是公式表中的 S'
    }
    public static double getSpectrumEntropy(Spectrum spectrum) {
        return getEntropy(spectrum.getInts());
    }

}
