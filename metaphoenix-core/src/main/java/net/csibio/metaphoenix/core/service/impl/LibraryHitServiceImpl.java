package net.csibio.metaphoenix.core.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.bean.common.Spectrum;
import net.csibio.metaphoenix.client.algorithm.entropy.Entropy;
import net.csibio.metaphoenix.client.algorithm.similarity.Similarity;
import net.csibio.metaphoenix.client.constants.Constants;
import net.csibio.metaphoenix.client.constants.enums.SpectrumMatchMethod;
import net.csibio.metaphoenix.client.domain.bean.identification.LibraryHit;
import net.csibio.metaphoenix.client.domain.bean.spectrum.IonPeak;
import net.csibio.metaphoenix.client.domain.db.MethodDO;
import net.csibio.metaphoenix.client.domain.db.SpectrumDO;
import net.csibio.metaphoenix.client.domain.query.SpectrumQuery;
import net.csibio.metaphoenix.client.service.LibraryHitService;
import net.csibio.metaphoenix.client.service.LibraryService;
import net.csibio.metaphoenix.client.service.SpectrumService;
import net.csibio.metaphoenix.client.utils.ArrayUtil;
import net.csibio.metaphoenix.client.utils.SpectrumUtil;
import org.apache.commons.math3.stat.StatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service("libraryHitService")
public class LibraryHitServiceImpl implements LibraryHitService {

    @Autowired
    SpectrumService spectrumService;
    @Autowired
    LibraryService libraryService;

    @Override
    public List<LibraryHit> getAllHits(SpectrumDO querySpectrumDO, String libraryId, Double mzTolerance, boolean isDecoy, SpectrumMatchMethod spectrumMatchMethod) {
        //  获取与查询谱图的前体质荷比在一定误差范围内匹配的库谱图列表
        //  libraryId 为 targetLibraryId
        List<LibraryHit> libraryHits = new ArrayList<>();
        List<IonPeak> ionPeaksA = null;
//        SpectrumDO newQuerySpectrumDO = TotalNoise(querySpectrumDO, 0.5, mzTolerance);
        SpectrumDO newQuerySpectrumDO = querySpectrumDO ;
        List<SpectrumDO> queSpectrumDOS = spectrumService.getByPrecursorMz(newQuerySpectrumDO.getPrecursorMz(), mzTolerance, libraryId);

//        if ((spectrumMatchMethod.getName().equals("IonEntropyCosine") || (spectrumMatchMethod.getName().equals("IonEntropySpecEntropy")) || (spectrumMatchMethod.getName().equals("IonEntropyRankCosineSimilarity"))) && !isDecoy) {
//            ionPeaksA = getCaculateionEntropy(querySpectrumDO,  queSpectrumDOS, mzTolerance);
//        }

        if ((spectrumMatchMethod.getName().equals("IonEntropyRankCosineSimilarity")) && !isDecoy) {
            ionPeaksA = getCaculateionEntropy(newQuerySpectrumDO,  queSpectrumDOS, mzTolerance);
        }

        for (SpectrumDO libSpectrumDO : queSpectrumDOS) {
            LibraryHit libraryHit = init(newQuerySpectrumDO, libSpectrumDO, isDecoy);
            if (ionPeaksA != null) {
                List<SpectrumDO> libSpectrumDOS = spectrumService.getByPrecursorMz(libSpectrumDO.getPrecursorMz(), mzTolerance, libraryId);
                List<IonPeak> ionPeaksB = getCaculateionEntropy(libSpectrumDO, libSpectrumDOS, mzTolerance);

                libraryHit.setScore(getScore(newQuerySpectrumDO, libSpectrumDO, mzTolerance, spectrumMatchMethod, ionPeaksA, ionPeaksB));
                libraryHits.add(libraryHit);
            } else {
                libraryHit.setScore(getScore(newQuerySpectrumDO, libSpectrumDO, mzTolerance, spectrumMatchMethod));
            }
            libraryHits.add(libraryHit);
        }

        return libraryHits;
    }

    @Override
    public ConcurrentHashMap<SpectrumDO, List<LibraryHit>> getTargetDecoyHitsMap(List<SpectrumDO> querySpectrumDOS, String targetLibraryId, String decoyLibraryId, MethodDO methodDO) {
        ConcurrentHashMap<SpectrumDO, List<LibraryHit>> hitsMap = new ConcurrentHashMap<>();
        querySpectrumDOS.parallelStream().forEach(spectrumDO -> {
            List<LibraryHit> targetDecoyHits = new ArrayList<>();
            double mzTolerance = methodDO.getPpmForMzTolerance() ? methodDO.getPpm() * spectrumDO.getPrecursorMz() * Constants.PPM : methodDO.getMzTolerance();
            List<LibraryHit> targetHits = getAllHits(spectrumDO, targetLibraryId, mzTolerance, false, methodDO.getSpectrumMatchMethod());
            //  mzTolerance是根据querySpectrum设置的容忍度
            List<LibraryHit> decoyHits = getAllHits(spectrumDO, decoyLibraryId, mzTolerance, true, methodDO.getSpectrumMatchMethod());
            targetDecoyHits.addAll(targetHits);
            targetDecoyHits.addAll(decoyHits);
            hitsMap.put(spectrumDO, targetDecoyHits);
        });
        return hitsMap;
    }

    private List<IonPeak> getCaculateionEntropy(SpectrumDO SpectrumDO, List<SpectrumDO> spectrumDOS, double mzTolerance) {
        List<IonPeak> Ionpeaksall = new ArrayList<>();
        List<IonPeak> ionWarehouse = new ArrayList<>();
        for (SpectrumDO spectrum : spectrumDOS) {
//            double baseIntensity = StatUtils.max(spectrum.getInts());
            int precursorIndex = ArrayUtil.findNearestIndex(spectrum.getMzs(), spectrum.getPrecursorMz());
            double precursorIntensity = spectrum.getInts()[precursorIndex];
            for (int i = 0; i < spectrum.getMzs().length; i++) {
                IonPeak ionPeak = new IonPeak(spectrum.getMzs()[i], spectrum.getInts()[i]  / precursorIntensity);
                ionWarehouse.add(ionPeak);
            }
        }

        HashMap<IonPeak, List<IonPeak>> ionPeakMap = new HashMap<>();
        for (int i = 0; i < SpectrumDO.getMzs().length; i++) {
            int precursorIndex = ArrayUtil.findNearestIndex(SpectrumDO.getMzs(), SpectrumDO.getPrecursorMz());
            double precursorIntensity = SpectrumDO.getInts()[precursorIndex];
//            double basePeakIntensity = StatUtils.max(SpectrumDO.getInts());

            IonPeak ionPeak = new IonPeak(SpectrumDO.getMzs()[i], SpectrumDO.getInts()[i] / precursorIntensity);
            Double ionMzTolerance = 10 * Constants.PPM * ionPeak.getMz();
            //  在离子峰集合中检索与当前离子峰在离子峰容忍度范围内的峰，添加到集合中
            List<IonPeak> ionPeaks = ionWarehouse.stream().filter(ion -> Math.abs(ion.getMz() - ionPeak.getMz()) < mzTolerance).toList();
            ionPeakMap.put(ionPeak, ionPeaks);
        }

        //calculate ion entropy
        for (IonPeak indexIonPeak : ionPeakMap.keySet()) {
            List<IonPeak> ionPeaks = ionPeakMap.get(indexIonPeak);
            double[] ionIntensities = new double[ionPeaks.size()];
            for (int i = 0; i < ionPeaks.size(); i++) {
                ionIntensities[i] = ionPeaks.get(i).getIntensity();
            }
            double ionEntropy = Entropy.getIonEntropy(ionIntensities);
            indexIonPeak.setIonEntropy(ionEntropy);
            Ionpeaksall.add(new IonPeak(indexIonPeak.getMz(), indexIonPeak.getIntensity(), ionEntropy));
        }
        return Ionpeaksall;
    }

    public SpectrumDO TotalNoise(SpectrumDO spectrumDO, double large, Double mzTolerance){
        Spectrum spectrum = SpectrumUtil.clone(spectrumDO.getSpectrum());

        int precursorIndex = ArrayUtil.findNearestIndex(spectrumDO.getMzs(), spectrumDO.getPrecursorMz());
        double precursorMz = spectrumDO.getMzs()[precursorIndex];
        double basePeak = StatUtils.max(spectrumDO.getInts());
        HashMap<Double, Double> spectrumMap = new HashMap<>();
        for (int i = 0; i < spectrum.getMzs().length; i++)
            spectrumMap.put(spectrum.getMzs()[i], spectrum.getInts()[i]);


        int noisePeakNum = (int) (large * spectrum.getMzs().length);
        if (noisePeakNum < 1)
            noisePeakNum = 1;
        double[] noiseMzs = new double[noisePeakNum];
        double[] noiseInts = new double[noisePeakNum];

        for(int i = 0; i < noisePeakNum; i++){
            noiseMzs[i] = generateRandomMz(precursorMz);
            noiseInts[i] = generateRandomIntensity(basePeak) * 0.01;
            spectrumMap.put(noiseMzs[i], noiseInts[i]);
        }

        List<Double> spectrumMzList = new ArrayList<>(spectrumMap.keySet().stream().toList());
        spectrumMzList.sort(Double::compareTo);
        double[] mzs = new double[spectrumMap.keySet().size()];
        double[] ints = new double[spectrumMap.keySet().size()];
        for (int i = 0; i < spectrumMzList.size(); i++) {
            mzs[i] = spectrumMzList.get(i);
            ints[i] = spectrumMap.get(mzs[i]);
        }
        spectrumDO.setMzs(mzs);
        spectrumDO.setInts(ints);
        return spectrumDO;
    }
    private static double generateRandomMz(double precursorMz) {
        Random random = new Random();
        return random.nextDouble() * precursorMz;
    }
    private static double generateRandomIntensity(double precursorInt) {
        Random random = new Random();
        return random.nextDouble() * precursorInt;
    }


    private LibraryHit init(SpectrumDO querySpectrumDO, SpectrumDO libSpectrumDO, boolean isDecoy) {
        LibraryHit libraryHit = new LibraryHit();
        libraryHit.setQuerySpectrumId(querySpectrumDO.getId());
        libraryHit.setLibSpectrumId(libSpectrumDO.getId()); //库谱图的 ID
        libraryHit.setCompoundName(libSpectrumDO.getCompoundName()); //化合物名
        libraryHit.setLibraryId(libSpectrumDO.getLibraryId());  //所属库的 ID
        libraryHit.setDecoy(isDecoy);
        libraryHit.setPrecursorAdduct(libSpectrumDO.getPrecursorAdduct());  //前体添加物
        libraryHit.setPrecursorMz(libSpectrumDO.getPrecursorMz());
        libraryHit.setSmiles(libSpectrumDO.getSmiles());
        libraryHit.setInChIKey(libSpectrumDO.getInChIKey());
        return libraryHit;
    }

    private Double getScore(SpectrumDO querySpectrumDO, SpectrumDO libSpectrumDO, Double mzTolerance, SpectrumMatchMethod spectrumMatchMethod) {
        int query_precursorIndex = ArrayUtil.findNearestIndex(querySpectrumDO.getMzs(), querySpectrumDO.getPrecursorMz());
        int lib_precursorIndex = ArrayUtil.findNearestIndex(libSpectrumDO.getMzs(), libSpectrumDO.getPrecursorMz());

        SpectrumDO removal_querySpectrumDO = new SpectrumDO();
        List<Double> query_mzs = new ArrayList<>();
        List<Double> query_ints = new ArrayList<>();

        for (int i = 0; i < querySpectrumDO.getMzs().length; i++) {
            if (i != query_precursorIndex) {
                query_mzs.add(querySpectrumDO.getMzs()[i]);
                query_ints.add(querySpectrumDO.getInts()[i]);
            }
        }
        removal_querySpectrumDO.setMzs(query_mzs.stream().mapToDouble(Double::doubleValue).toArray());
        removal_querySpectrumDO.setInts(query_ints.stream().mapToDouble(Double::doubleValue).toArray());

        SpectrumDO removal_libSpectrumDO = new SpectrumDO();
        List<Double> lib_mzs = new ArrayList<>();
        List<Double> lib_ints = new ArrayList<>();
        for (int i = 0; i < libSpectrumDO.getMzs().length; i++) {
            if (i != lib_precursorIndex) {
                lib_mzs.add(libSpectrumDO.getMzs()[i]);
                lib_ints.add(libSpectrumDO.getInts()[i]);
            }
        }
        removal_libSpectrumDO.setMzs(lib_mzs.stream().mapToDouble(Double::doubleValue).toArray());
        removal_libSpectrumDO.setInts(lib_ints.stream().mapToDouble(Double::doubleValue).toArray());

        return switch (spectrumMatchMethod) {
            case Entropy ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Entropy, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Entropy, mzTolerance);
            case Cosine ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Cosine, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Cosine, mzTolerance);
            case IonEntropyCosine ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyCosine, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyCosine, mzTolerance);
            case IonEntropySpecEntropy ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropySpecEntropy, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropySpecEntropy, mzTolerance);
            case IonEntropyRankCosineSimilarity ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyRankCosineSimilarity, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyRankCosineSimilarity, mzTolerance);
            case Unweighted_Entropy ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Unweighted_Entropy, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Unweighted_Entropy, mzTolerance);
//            case MetaPro ->
//                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.MetaPro, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.MetaPro, mzTolerance);
            case Weighted_Cosine ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Weighted_Cosine, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Weighted_Cosine, mzTolerance);
            case Euclidean ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Euclidean, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Euclidean, mzTolerance);
            case Manhattan ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Manhattan, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.Manhattan, mzTolerance);
            case RankCosine ->
                    Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.RankCosine, mzTolerance);
//                    Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.RankCosine, mzTolerance);
        };
    }
    private Double getScore(SpectrumDO querySpectrumDO, SpectrumDO libSpectrumDO, Double mzTolerance, SpectrumMatchMethod spectrumMatchMethod,List<IonPeak> ionPeaksA, List<IonPeak> ionPeaksB) {
        int query_precursorIndex = ArrayUtil.findNearestIndex(querySpectrumDO.getMzs(), querySpectrumDO.getPrecursorMz());
        int lib_precursorIndex = ArrayUtil.findNearestIndex(libSpectrumDO.getMzs(), libSpectrumDO.getPrecursorMz());

        SpectrumDO removal_querySpectrumDO = new SpectrumDO();
        List<Double> query_mzs = new ArrayList<>();
        List<Double> query_ints = new ArrayList<>();

        for (int i = 0; i < querySpectrumDO.getMzs().length; i++) {
            if (i != query_precursorIndex) {
                query_mzs.add(querySpectrumDO.getMzs()[i]);
                query_ints.add(querySpectrumDO.getInts()[i]);
            }
        }
        removal_querySpectrumDO.setMzs(query_mzs.stream().mapToDouble(Double::doubleValue).toArray());
        removal_querySpectrumDO.setInts(query_ints.stream().mapToDouble(Double::doubleValue).toArray());

        SpectrumDO removal_libSpectrumDO = new SpectrumDO();
        List<Double> lib_mzs = new ArrayList<>();
        List<Double> lib_ints = new ArrayList<>();
        for (int i = 0; i < libSpectrumDO.getMzs().length; i++) {
            if (i != lib_precursorIndex) {
                lib_mzs.add(libSpectrumDO.getMzs()[i]);
                lib_ints.add(libSpectrumDO.getInts()[i]);
            }
        }
        removal_libSpectrumDO.setMzs(lib_mzs.stream().mapToDouble(Double::doubleValue).toArray());
        removal_libSpectrumDO.setInts(lib_ints.stream().mapToDouble(Double::doubleValue).toArray());

        if (spectrumMatchMethod.getName().equals("IonEntropyCosine"))
            return Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyCosine, mzTolerance, ionPeaksA, ionPeaksB);
//            return Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyCosine, mzTolerance, ionPeaksA, ionPeaksB);
        else if(spectrumMatchMethod.getName().equals("IonEntropySpecEntropy"))
            return Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropySpecEntropy, mzTolerance, ionPeaksA, ionPeaksB);
//            return Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropySpecEntropy, mzTolerance, ionPeaksA, ionPeaksB);
        else
            return Similarity.getScore(querySpectrumDO.getSpectrum(), libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyRankCosineSimilarity, mzTolerance, ionPeaksA, ionPeaksB);
//            return Similarity.getScore(removal_querySpectrumDO.getSpectrum(), removal_libSpectrumDO.getSpectrum(), SpectrumMatchMethod.IonEntropyRankCosineSimilarity, mzTolerance, ionPeaksA, ionPeaksB);
    }
}
