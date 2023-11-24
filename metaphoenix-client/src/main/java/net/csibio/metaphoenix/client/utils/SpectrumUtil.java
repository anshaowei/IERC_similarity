package net.csibio.metaphoenix.client.utils;

import net.csibio.aird.bean.common.Spectrum;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SpectrumUtil {

    /**
     * normalize the spectrum
     */
    public static void normalize(Spectrum spectrum) {
        double sum = ArrayUtil.sum(spectrum.getInts()); //对谱图的强度数组进行求和
        ArrayUtil.normalize(spectrum.getInts(), sum);   //将谱图的intensity进行归一化处理
    }

    public static void Ionnormalize(Spectrum spectrum) {
        double BaseIntensity = StatUtils.max(spectrum.getInts());
        ArrayUtil.Ionnormalize(spectrum.getInts(), BaseIntensity);   //将谱图的intensity进行归一化处理
    }
    /**
     * mix two spectrum by the given weight, mixIntensity = intensity1 * weight1 + intensity2 * weight2
     */
    public static Spectrum mixByWeight(Spectrum spectrum1, Spectrum spectrum2, double weight1, double weight2, double mzTolerance) {

        HashMap<Double, Double> mixMap = new HashMap<>();
        HashMap<Double, Double> spectrum2Map = new HashMap<>();
        List<Double> spectrum2MzList = new ArrayList<>();
        for (int i = 0; i < spectrum2.getMzs().length; i++) {
            spectrum2MzList.add(spectrum2.getMzs()[i]);
            spectrum2Map.put(spectrum2.getMzs()[i], spectrum2.getInts()[i]);
        }

        //match mz in the two spectra
        for (int i = 0; i < spectrum1.getMzs().length; i++) {
            double mz1 = spectrum1.getMzs()[i];
            List<Double> candidateMzList = spectrum2MzList.stream().filter(mz2 -> Math.abs(mz1 - mz2) <= mzTolerance).toList();
            if (!candidateMzList.isEmpty()) {
                double diff = Double.MAX_VALUE;
                double mz2 = 0;
                for (double candidate : candidateMzList) {
                    double tempDiff = Math.abs(mz1 - candidate);
                    if (tempDiff < diff) {
                        mz2 = candidate;
                        diff = tempDiff;
                    }
                }
                double mixMz = (mz1 + mz2) / 2;
                double mixIntensity = spectrum1.getInts()[i] * weight1 + spectrum2Map.get(mz2) * weight2;
//                double mixIntensity = Math.pow(spectrum1.getInts()[i], weight1) + Math.pow(spectrum2Map.get(mz2), weight2);
                mixMap.put(mixMz, mixIntensity);
                spectrum2MzList.remove(mz2);
            } else {
                mixMap.put(mz1, spectrum1.getInts()[i] * weight1);
//                mixMap.put(mz1, Math.pow(spectrum1.getInts()[i], weight1));
            }
        }

        //add the rest of mz2List
        for (double mz2 : spectrum2MzList) {
            mixMap.put(mz2, spectrum2Map.get(mz2));
        }

        //turn mixMap to mixSpectrum
        List<Double> mixMzList = new ArrayList<>(mixMap.keySet().stream().toList());
        mixMzList.sort(Double::compareTo);  //保证m/z值按照升序排列
        double[] mzs = new double[mixMap.keySet().size()];
        double[] ints = new double[mixMap.keySet().size()];
        for (int i = 0; i < mixMzList.size(); i++) {
            mzs[i] = mixMzList.get(i);
            ints[i] = mixMap.get(mzs[i]);
        }
        return new Spectrum(mzs, ints);
    }

    public static Spectrum clone(Spectrum spectrum) {
        // 创建原始质谱谱图的一个副本，可以在对质谱谱图进行处理或修改时使用，而不影响原始谱图对象
        Spectrum copySpectrum = new Spectrum(new double[spectrum.getMzs().length], new double[spectrum.getInts().length]);
        // 创建一个新的Spectrum对象copySpectrum，它的m/z和intensity数组长度与输入的spectrum相同
        System.arraycopy(spectrum.getMzs(), 0, copySpectrum.getMzs(), 0, spectrum.getMzs().length);
        System.arraycopy(spectrum.getInts(), 0, copySpectrum.getInts(), 0, spectrum.getInts().length);
        // 使用System.arraycopy方法将原始spectrum对象的m/z 和 intensity 数组复制到新对象copySpectrum的m/z数组中
        return copySpectrum;
        // 返回新的copySpectrum对象，它是原始spectrum对象的一个副本，两个对象的m/z和强度数组相同
    }

}
