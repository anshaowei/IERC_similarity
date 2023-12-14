package net.csibio.metaphoenix.client.algorithm.similarity;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.common.Spectrum;
import net.csibio.metaphoenix.client.constants.enums.SpectrumMatchMethod;
import net.csibio.metaphoenix.client.domain.bean.spectrum.IonPeak;
import net.csibio.metaphoenix.client.utils.ArrayUtil;
import net.csibio.metaphoenix.client.utils.SpectrumUtil;
import net.csibio.metaphoenix.client.algorithm.entropy.Entropy;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.reverseOrder;

@Slf4j
public class Similarity {

    public static double getScore(Spectrum querySpectrum, Spectrum libSpectrum, SpectrumMatchMethod spectrumMatchMethod, double mzTolerance) {
        double score = 0;
        switch (spectrumMatchMethod) {
            case Cosine:
                score = getCosineSimilarity(querySpectrum, libSpectrum, mzTolerance, false);
                break;
            case Entropy:
                score = getEntropySimilarity(querySpectrum, libSpectrum, mzTolerance);
                break;
            case Unweighted_Entropy:
                score = getUnWeightedEntropySimilarity(querySpectrum, libSpectrum, mzTolerance);
                break;
            //case MetaPro:
            //    score = getMetaProSimilarity(querySpectrum, libSpectrum, mzTolerance);
            //    break;
            case Weighted_Cosine:
                score = getCosineSimilarity(querySpectrum, libSpectrum, mzTolerance, true);
                break;
            case Euclidean:
                score = getEuclideanSimilarity(querySpectrum, libSpectrum, mzTolerance, false);
                break;
            case Manhattan:
                score = getManhattanSimilarity(querySpectrum, libSpectrum, mzTolerance);
                break;
            case RankCosine:
                score = getRankCosineSimilarity(querySpectrum, libSpectrum, mzTolerance);
                break;
            default:
                break;
        }
        if (score < 0) {
            score = 0.0;
        }
        if (score > 1) {
            score = 1.0;
        }
        return score;
    }
    public static double getScore(Spectrum querySpectrum, Spectrum libSpectrum, SpectrumMatchMethod spectrumMatchMethod, double mzTolerance, List<IonPeak> ionPeaksA, List<IonPeak> ionPeaksB){
        double score = 0;
        switch (spectrumMatchMethod){
            case IonEntropyCosine:
                score = getIonEntropyCosineSimilarity(querySpectrum, libSpectrum, mzTolerance, ionPeaksA, ionPeaksB);
                break;
            case IonEntropySpecEntropy:
                score = getIonEntropySpecEntropySimilarity(querySpectrum, libSpectrum, mzTolerance, ionPeaksA, ionPeaksB);
                break;
            case IonEntropyRankCosineSimilarity:
                score = getIonEntropyRankCosineSimilarity(querySpectrum, libSpectrum, mzTolerance, ionPeaksA, ionPeaksB);
                break;
        }
        if (score < 0) {
            score = 0.0;
        }
        if (score > 1) {
            score = 1.0;
        }
        return score;
    }
    public static double getIonEntropySpecEntropySimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance, List<IonPeak> ionPeaksA, List<IonPeak> ionPeaksB) {
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        for (int i = 0; i < spectrumA.getMzs().length; i++){
            double mz = spectrumA.getMzs()[i];
            double intensity = spectrumA.getInts()[i];
            double ionEntropyA = 0d;
            double weight = 0d;
            IonPeak ionPeakA = getIonPeakForMz(mz, mzTolerance, ionPeaksA);
            if (ionPeakA != null) {
                ionEntropyA = ionPeakA.getIonEntropy();
                weight = Math.exp(-ionEntropyA);
            }
            if (ionEntropyA >= 1d) {
                weight = 0;
            }
            spectrumA.getInts()[i] = intensity * weight;
        }

        for (int i = 0; i < spectrumB.getMzs().length; i++){
            double mz = spectrumB.getMzs()[i];
            double intensity = spectrumB.getInts()[i];
            double ionEntropyB = 0d;
            double weight = 0d;
            IonPeak ionPeakB = getIonPeakForMz(mz, mzTolerance, ionPeaksB);
            if (ionPeakB != null) {
                ionEntropyB = ionPeakB.getIonEntropy();
                weight = Math.exp(-ionEntropyB);
            }
            if (ionEntropyB >= 1d) {
                weight = 0;
            }
            spectrumB.getInts()[i] = intensity * weight;
        }

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0;
        double peakBInt = 0d;

        while (expIndex < expMzArray.length && libIndex < libMzArray.length) {
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], peakBInt});
                peakBInt = 0.0;
                expIndex++;
            } else if (expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance) {
                specMerged.add(new double[]{libMzArray[libIndex], 0.0, libIntArray[libIndex]});
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], peakBInt});
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length) {
            specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], 0.0});
            expIndex++;
        }
        while (libIndex < libMzArray.length) {
            specMerged.add(new double[]{libMzArray[libIndex], 0.0, libIntArray[libIndex]});
            libIndex++;
        }

        double[] a = new double[specMerged.size()];
        double[] b = new double[specMerged.size()];
        double[] mix = new double[specMerged.size()];

        double[][] result = new double[specMerged.size()][3];

        for (int i = 0; i < specMerged.size(); i++) {
            result[i] = specMerged.get(i);
            a[i] = result[i][1];
            b[i] = result[i][2];
            mix[i] = result[i][1] + result[i][2];
        }
        double entropyA = Entropy.getEntropy(a);
        double entropyB = Entropy.getEntropy(b);
        double entropyMix;

        double[] weighted_intensityA;
        double[] weighted_intensityB;
        if (entropyA < 3) {
            weighted_intensityA = getEntropyMix(a, entropyA);
            entropyA = Entropy.getEntropy(weighted_intensityA);
        } else {
            weighted_intensityA = a;
        }

        if (entropyB < 3) {
            weighted_intensityB = getEntropyMix(b, entropyB);
            entropyB = Entropy.getEntropy(weighted_intensityB);
        } else {
            weighted_intensityB = b;
        }

        double[] weighted_intensityMix = new double[specMerged.size()];
        for (int i = 0; i < specMerged.size(); i++) {
            weighted_intensityMix[i] = weighted_intensityA[i] + weighted_intensityB[i];
        }

        entropyMix = Entropy.getEntropy(weighted_intensityMix);


        return 1 - (2 * entropyMix - entropyA - entropyB) / Math.log(4);
    }
    public static double getIonEntropyCosineSimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance, List<IonPeak> ionPeaksA, List<IonPeak> ionPeaksB) {
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        for (int i = 0; i < spectrumA.getMzs().length; i++){
            double mz = spectrumA.getMzs()[i];
            double intensity = spectrumA.getInts()[i];
            double ionEntropyA = 0d;
            double weight = 0d;
            IonPeak ionPeakA = getIonPeakForMz(mz, mzTolerance, ionPeaksA);
            if (ionPeakA != null) {
                ionEntropyA = ionPeakA.getIonEntropy();
                weight = Math.exp(-ionEntropyA);
            }
            if (ionEntropyA >= 1d) {
                weight = 0;
            }
            spectrumA.getInts()[i] = intensity * weight;
        }

        for (int i = 0; i < spectrumB.getMzs().length; i++){
            double mz = spectrumB.getMzs()[i];
            double intensity = spectrumB.getInts()[i];
            double ionEntropyB = 0d;
            double weight = 0d;
            IonPeak ionPeakB = getIonPeakForMz(mz, mzTolerance, ionPeaksB);
            if (ionPeakB != null) {
                ionEntropyB = ionPeakB.getIonEntropy();
                weight = Math.exp(-ionEntropyB);
            }
            if (ionEntropyB >= 1d) {
                weight = 0;
            }
            spectrumB.getInts()[i] = intensity * weight;
        }

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0;
        double peakBInt = 0d, dotProduct = 0d, expNorm = 0d, libNorm = 0d;
        while (expIndex < expMzArray.length && libIndex < libMzArray.length){
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
                peakBInt = 0.0;
                expIndex++;
            } else if(expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance){
                specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length){
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], 0.0 });
            expIndex++;
        }
        while (libIndex < libMzArray.length){
            specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
            libIndex++;
        }

        double[][] result = new double[specMerged.size()][3];
        for (int i = 0; i < specMerged.size(); i++)
            result[i] = specMerged.get(i);

        for (double[] doubles : result) {
            double mz = doubles[0];
            double intensity1 = doubles[1];
            double intensity2 = doubles[2];
            dotProduct += intensity1 * intensity2;
            expNorm += intensity1 * intensity1;
            libNorm += intensity2 * intensity2;
        }
        return (dotProduct * dotProduct) / (expNorm * libNorm);
    }

    public static IonPeak getIonPeakForMz(double mz, double mzTolerance, List<IonPeak> ionPeaks) {
        for (IonPeak ionPeak : ionPeaks) {
            if (Math.abs(ionPeak.getMz() - mz) < mzTolerance) {
                return ionPeak;
            }
        }
        return null; // 如果没有找到对应质荷比的离子峰，返回 null 或者其他默认值
    }

    private static double getIonEntropyRankCosineSimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance, List<IonPeak> ionPeaksA, List<IonPeak> ionPeaksB) {
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.Ionnormalize(spectrumA);
        SpectrumUtil.Ionnormalize(spectrumB);

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0;
        double peakBInt = 0d, dotProduct = 0d, expNorm = 0d, libNorm = 0d;
        while (expIndex < expMzArray.length && libIndex < libMzArray.length){
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
                peakBInt = 0.0;
                expIndex++;
            } else if(expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance){
                specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length){
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], 0.0 });
            expIndex++;
        }
        while (libIndex < libMzArray.length){
            specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
            libIndex++;
        }

        double[][] result = new double[specMerged.size()][3];
        for (int i = 0; i < specMerged.size(); i++)
            result[i] = specMerged.get(i);


        Arrays.sort(result, Comparator.comparingDouble((double[] a) -> a[2]));
        for (int i=result.length-1; i>=0; i--)
            if (result[i][2] != 0)
                result[i][2] = i+1;

        Arrays.sort(result, Comparator.comparingDouble((double[] a) -> a[1]));
        for (int i=result.length-1; i >=0; i--)
            if (result[i][1] != 0)
                result[i][1] = i+1;


        for (double[] doubles : result) {
            double mz = doubles[0];
            double intensity1 = doubles[1];
            double intensity2 = doubles[2];
            double weight1 = 0d;
            double weight2 = 0d;

            IonPeak ionPeakA = getIonPeakForMz(mz, mzTolerance, ionPeaksA); // ionPeaksA是谱图A的离子熵列表
            double ionEntropyA = 0d;
            double ionEntropyB = 0d;
            if (ionPeakA != null) {
                ionEntropyA = ionPeakA.getIonEntropy();
                weight1 = Math.exp(-ionEntropyA);
                if (ionEntropyA >= 1d) {
                    weight1 = 0;
                }
            }
            IonPeak ionPeakB = getIonPeakForMz(mz, mzTolerance, ionPeaksB); // ionPeaksB是谱图B的离子熵列表
            if (ionPeakB != null) {
                ionEntropyB = ionPeakB.getIonEntropy();
                weight2 = Math.exp(-ionEntropyB);
                if (ionEntropyB >= 1d) {
                    weight2 = 0;
                }
            }

            dotProduct += intensity1 * intensity2 * weight1 * weight2;
            expNorm += intensity1 * intensity1 * weight1 * weight1;
            libNorm += intensity2 * intensity2 * weight2 * weight2;
        }
        return Math.sqrt(dotProduct * dotProduct) / Math.sqrt(expNorm * libNorm);
    }
    private static double getManhattanSimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance){
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0, matchCount = 0;
        double peakBInt = 0d, Manhattan=0d;

        while (expIndex < expMzArray.length && libIndex < libMzArray.length){
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
                peakBInt = 0.0;
                expIndex++;
            } else if(expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance){
                specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
            peakBInt = 0;
            expIndex++;
        }
        while (expIndex < expMzArray.length){
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], 0.0 });
            expIndex++;
        }
        while (libIndex < libMzArray.length) {
            specMerged.add(new double[]{libMzArray[libIndex], 0.0, libIntArray[libIndex]});
            libIndex++;
        }

        double[][] result = new double[specMerged.size()][3];
        for (int i = 0; i < specMerged.size(); i++)
            result[i] = specMerged.get(i);

        for (double[] doubles : result) {
            double intensity1 = doubles[1];
            double intensity2 = doubles[2];
            Manhattan += Math.abs(intensity1 - intensity2);
        }
        return 1 - Manhattan/2 ;
    }

    private static double getMetaProSimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance) {
        int expIndex = 0;
        double[] libMzArray = libSpectrum.getMzs();
        double[] libIntArray = libSpectrum.getInts();
        double[] expMzArray = querySpectrum.getMzs();
        double[] expIntArray = querySpectrum.getInts();

        //librarySpectrum的最大值
        double maxLibIntensity = StatUtils.max(libIntArray);

        int libCounter = 0, expCounter = 0;
        double dotProduct = 0d, libNorm = 0d, expNorm = 0d;
        for (int libIndex = 0; libIndex < libMzArray.length; libIndex++) {
            double leftMz = libMzArray[libIndex] - mzTolerance;
            double rightMz = libMzArray[libIndex] + mzTolerance;

            //统计lib中大于最大值百分之一的部分
            double libIntensity = libIntArray[libIndex];
            if (libIntensity < 0.01 * maxLibIntensity) {
                continue;
            }
            int libBinWidth = 1;
            while (libIndex + libBinWidth < libMzArray.length && libMzArray[libIndex + libBinWidth] < rightMz) {
                libIntensity += libIntArray[libIndex + libBinWidth];
                libBinWidth++;
            }
            libIndex += libBinWidth - 1;
            libCounter++;
            //统计exp中和lib相对应的部分
            double expIntensity = 0;
            for (; expIndex < expMzArray.length; expIndex++) {
                if (expMzArray[expIndex] < leftMz) {
                    continue;
                }
                if (leftMz <= expMzArray[expIndex] && expMzArray[expIndex] < rightMz) {
                    expIntensity += expIntArray[expIndex];
                } else {
                    break;
                }
            }
            libNorm += libIntensity * libIntensity;
            if (expIntensity > 0) {
                expCounter++;
            }
            expNorm += expIntensity * expIntensity;
            dotProduct += expIntensity * libIntensity;
        }
        if (libNorm == 0 || expNorm == 0 || libCounter == 0) {
            return 0;
        }
        return dotProduct / Math.sqrt(libNorm) / Math.sqrt(expNorm) * expCounter / libCounter;
    }

    private static double getUnWeightedEntropySimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance) {
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0;
        double peakBInt = 0d;

        while (expIndex < expMzArray.length && libIndex < libMzArray.length) {
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], peakBInt});
                peakBInt = 0.0;
                expIndex++;
            } else if (expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance) {
                specMerged.add(new double[]{libMzArray[libIndex], 0.0, libIntArray[libIndex]});
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], peakBInt});
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length) {
            specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], 0.0});
            expIndex++;
        }
        while (libIndex < libMzArray.length) {
            specMerged.add(new double[]{libMzArray[libIndex], 0.0, libIntArray[libIndex]});
            libIndex++;
        }

        double[] a = new double[specMerged.size()];
        double[] b = new double[specMerged.size()];
        double[] mix = new double[specMerged.size()];

        double[][] result = new double[specMerged.size()][3];

        for (int i = 0; i < specMerged.size(); i++) {
            result[i] = specMerged.get(i);
            a[i] = result[i][1];
            b[i] = result[i][2];
            mix[i] = result[i][1] + result[i][2];
        }
        double entropyA = Entropy.getEntropy(a);
        double entropyB = Entropy.getEntropy(b);
        double entropyMix;

        double[] weighted_intensityA;
        double[] weighted_intensityB;
        weighted_intensityA = a;
        weighted_intensityB = b;

        double[] weighted_intensityMix = new double[weighted_intensityA.length];
        for (int i = 0; i < weighted_intensityA.length; i++)
            weighted_intensityMix[i] = weighted_intensityA[i] + weighted_intensityB[i];

        entropyMix = Entropy.getEntropy(weighted_intensityMix);

        return 1 - (2 * entropyMix - entropyA - entropyB) / Math.log(4);
    }

    private static double getEntropySimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance) {
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0;
        double peakBInt = 0d;

        while (expIndex < expMzArray.length && libIndex < libMzArray.length) {
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], peakBInt});
                peakBInt = 0.0;
                expIndex++;
            } else if (expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance) {
                specMerged.add(new double[]{libMzArray[libIndex], 0.0, libIntArray[libIndex]});
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], peakBInt});
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length) {
            specMerged.add(new double[]{expMzArray[expIndex], expIntArray[expIndex], 0.0});
            expIndex++;
        }
        while (libIndex < libMzArray.length) {
            specMerged.add(new double[]{libMzArray[libIndex], 0.0, libIntArray[libIndex]});
            libIndex++;
        }

        double[] a = new double[specMerged.size()];
        double[] b = new double[specMerged.size()];
        double[] mix = new double[specMerged.size()];

        double[][] result = new double[specMerged.size()][3];

        for (int i = 0; i < specMerged.size(); i++) {
            result[i] = specMerged.get(i);
            a[i] = result[i][1];
            b[i] = result[i][2];
            mix[i] = result[i][1] + result[i][2];
        }
        double entropyA = Entropy.getEntropy(a);
        double entropyB = Entropy.getEntropy(b);
        double entropyMix;

        double[] weighted_intensityA;
        double[] weighted_intensityB;
        if (entropyA < 3) {
            weighted_intensityA = getEntropyMix(a, entropyA);
            entropyA = Entropy.getEntropy(weighted_intensityA);
        } else {
            weighted_intensityA = a;
        }

        if (entropyB < 3) {
            weighted_intensityB = getEntropyMix(b, entropyB);
            entropyB = Entropy.getEntropy(weighted_intensityB);
        } else {
            weighted_intensityB = b;
        }

        double[] weighted_intensityMix = new double[weighted_intensityA.length];
        for (int i = 0; i < weighted_intensityA.length; i++)
            weighted_intensityMix[i] = weighted_intensityA[i] + weighted_intensityB[i];

        entropyMix = Entropy.getEntropy(weighted_intensityMix);

        return 1 - (2 * entropyMix - entropyA - entropyB) / Math.log(4);
    }

    private static double[] getEntropyMix(double[] intensity, double entropyMix) {
        double weightMix;
        weightMix = 0.25 + entropyMix * 0.25;
        double[] powerl = new double[intensity.length];
        for (int i = 0; i < intensity.length; i++){
            powerl[i] = Math.pow(intensity[i] , weightMix);
        }

        double sum = ArrayUtil.sum(powerl); //对谱图的强度数组进行求和
        ArrayUtil.normalize(powerl, sum);   //将谱图的intensity进行归一化处理

        return powerl;
    }
    private static double getCosineSimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance, boolean isWeighted) {
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0, matchCount = 0;
        double peakBInt = 0d, dotProduct = 0d, expNorm = 0d, libNorm = 0d;
        while (expIndex < expMzArray.length && libIndex < libMzArray.length){
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
                peakBInt = 0.0;
                expIndex++;
            } else if(expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance){
                specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length){
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], 0.0 });
            expIndex++;
        }
        while (libIndex < libMzArray.length){
            specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
            libIndex++;
        }

        double[][] result = new double[specMerged.size()][3];
        for (int i = 0; i < specMerged.size(); i++)
            result[i] = specMerged.get(i);

        for (double[] doubles : result) {
            double intensity1 = doubles[1];
            double intensity2 = doubles[2];
            if(isWeighted){
                dotProduct += weightedDotProduct(doubles[0], intensity1) * weightedDotProduct(doubles[0], intensity2);
                expNorm += weightedDotProduct(doubles[0], intensity1) * weightedDotProduct(doubles[0], intensity1);
                libNorm += weightedDotProduct(doubles[0], intensity2) * weightedDotProduct(doubles[0], intensity2);
            } else{
                dotProduct += intensity1 * intensity2;
                expNorm += intensity1 * intensity1;
                libNorm += intensity2 * intensity2;
            }
        }
        return (dotProduct * dotProduct) / (expNorm * libNorm);
        //        return dotProduct / Math.sqrt(expNorm * libNorm);
    }

    private static double getRankCosineSimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance) {
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();


        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0;
        double peakBInt = 0d, dotProduct = 0d, expNorm = 0d, libNorm = 0d;
        while (expIndex < expMzArray.length && libIndex < libMzArray.length){
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
                peakBInt = 0.0;
                expIndex++;
            } else if(expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance){
                specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length){
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], 0.0 });
            expIndex++;
        }
        while (libIndex < libMzArray.length){
            specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
            libIndex++;
        }

        double[][] result = new double[specMerged.size()][3];
        for (int i = 0; i < specMerged.size(); i++)
            result[i] = specMerged.get(i);

        Arrays.sort(result, Comparator.comparingDouble((double[] a) -> a[1]));
        double change_1 = 1 ;
        for (int i=0; i < result.length; i++) {
            if (result[i][1] != 0){
                result[i][1] = change_1;
                change_1++;
            }
        }
        Arrays.sort(result, Comparator.comparingDouble((double[] a) -> a[2]));
        double change_2 = 1 ;
        for (int i=0; i < result.length; i++) {
            if (result[i][2] != 0){
                result[i][2] = change_2;
                change_2++;
            }
        }

        for (double[] doubles : result) {
            double intensity1 = doubles[1];
            double intensity2 = doubles[2];
            dotProduct += intensity1 * intensity2;
            expNorm += intensity1 * intensity1;
            libNorm += intensity2 * intensity2;
        }
        return (dotProduct) / Math.sqrt(expNorm * libNorm);
        //        return (dotProduct * dotProduct) / (expNorm * libNorm);

    }

    private static double getEuclideanSimilarity(Spectrum querySpectrum, Spectrum libSpectrum, double mzTolerance, boolean Squared ){
        Spectrum spectrumA = SpectrumUtil.clone(querySpectrum);
        Spectrum spectrumB = SpectrumUtil.clone(libSpectrum);
        SpectrumUtil.normalize(spectrumA);
        SpectrumUtil.normalize(spectrumB);

        double[] libMzArray = spectrumB.getMzs();
        double[] libIntArray = spectrumB.getInts();
        double[] expMzArray = spectrumA.getMzs();
        double[] expIntArray = spectrumA.getInts();

        List<double[]> specMerged = new ArrayList<>();
        int expIndex = 0, libIndex = 0, matchCount = 0;
        double peakBInt = 0d, euclidean=0d;

        while (expIndex < expMzArray.length && libIndex < libMzArray.length){
            if (expMzArray[expIndex] < libMzArray[libIndex] - mzTolerance) {
                specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
                peakBInt = 0.0;
                expIndex++;
            } else if(expMzArray[expIndex] > libMzArray[libIndex] + mzTolerance){
                specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
                libIndex++;
            } else {
                peakBInt += libIntArray[libIndex];
                libIndex++;
            }
        }
        if (peakBInt > 0) {
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], peakBInt });
            peakBInt = 0;
            expIndex++;
        }

        while (expIndex < expMzArray.length){
            specMerged.add(new double[]{ expMzArray[expIndex], expIntArray[expIndex], 0.0 });
            expIndex++;
        }
        while (libIndex < libMzArray.length){
            specMerged.add(new double[]{ libMzArray[libIndex], 0.0, libIntArray[libIndex] });
            libIndex++;
        }

        double[][] result = new double[specMerged.size()][3];
        for (int i = 0; i < specMerged.size(); i++)
            result[i] = specMerged.get(i);

        for (double[] doubles : result) {
            double intensity1 = doubles[1];
            double intensity2 = doubles[2];
            euclidean += (intensity1 - intensity2) * (intensity1 - intensity2) ;
        }

        if(Squared)
            return  1 - (euclidean / 2);
        else
            return 1 - (Math.sqrt(euclidean) / Math.sqrt(2));
    }

    private static double weightedDotProduct(double mz, double intensity) {
        return Math.pow(mz, 3) * Math.pow(intensity, 0.6);

    }

}