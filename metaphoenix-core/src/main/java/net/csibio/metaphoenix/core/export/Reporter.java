package net.csibio.metaphoenix.core.export;

import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import net.csibio.aird.constant.SymbolConst;
import net.csibio.metaphoenix.client.algorithm.entropy.Entropy;
import net.csibio.metaphoenix.client.constants.Constants;
import net.csibio.metaphoenix.client.constants.enums.SpectrumMatchMethod;
import net.csibio.metaphoenix.client.domain.bean.identification.LibraryHit;
import net.csibio.metaphoenix.client.domain.bean.spectrum.IonPeak;
import net.csibio.metaphoenix.client.domain.db.MethodDO;
import net.csibio.metaphoenix.client.domain.db.SpectrumDO;
import net.csibio.metaphoenix.client.service.LibraryHitService;
import net.csibio.metaphoenix.client.service.SpectrumService;
import net.csibio.metaphoenix.client.utils.ArrayUtil;
import net.csibio.metaphoenix.core.config.VMProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component("reporter")
@Slf4j
public class Reporter {

    @Autowired
    VMProperties vmProperties;
    @Autowired
    SpectrumService spectrumService;
    @Autowired
    LibraryHitService libraryHitService;
    @Autowired
    MongoTemplate mongoTemplate;


    public void compareSpectrumMatchMethods(List<SpectrumDO> querySpectrumDOS, String targetLibraryId, MethodDO methodDO, int scoreInterval) {
        String fileName_FDR = "SpectrumMatchMethodComparison";
        String fileName_FPR = "FPRSpectrumMatchMethodComparison";
        String fileName_TPR = "TPRSpectrumMatchMethodComparison";
        String fileName_Pre = "PrecisionSpectrumMatchMethodComparison";
        String fileName_AUC = "AUCSpectrumMatchMethodComparison";

        String outputFileName_FDR = vmProperties.getRepository() + File.separator + fileName_FDR + ".xlsx";
        String outputFileName_FPR = vmProperties.getRepository() + File.separator + fileName_FPR + ".xlsx";
        String outputFileName_TPR = vmProperties.getRepository() + File.separator + fileName_TPR + ".xlsx";
        String outputFileName_Pre = vmProperties.getRepository() + File.separator + fileName_Pre + ".xlsx";
        String outputFileName_AUC = vmProperties.getRepository() + File.separator + fileName_AUC + ".xlsx";

        log.info("start export {} to {}", fileName_FDR, outputFileName_FDR);
        log.info("start export {} to {}", fileName_FPR, outputFileName_FPR);
        log.info("start export {} to {}", fileName_TPR, outputFileName_TPR);
        log.info("start export {} to {}", fileName_Pre, outputFileName_Pre);
        log.info("start export {} to {}", fileName_AUC, outputFileName_AUC);

        //init
        List<List<Object>> compareSheetFDR = new ArrayList<>();
        List<List<Object>> compareSheetFPR = new ArrayList<>();
        List<List<Object>> compareSheetTPR = new ArrayList<>();
        List<List<Object>> compareSheetPre = new ArrayList<>();
        List<List<Object>> compareSheetAUC = new ArrayList<>();

        for (int i = 0; i < scoreInterval; i++) {
            List<Object> rowFDR = new ArrayList<>();
            List<Object> rowFPR = new ArrayList<>();
            List<Object> rowTPR = new ArrayList<>();
            List<Object> rowPre = new ArrayList<>();
            List<Object> rowAUC = new ArrayList<>();
            compareSheetFDR.add(rowFDR);
            compareSheetFPR.add(rowFPR);
            compareSheetTPR.add(rowTPR);
            compareSheetPre.add(rowPre);
            compareSheetAUC.add(rowAUC);
        }
        List<Object> header = new ArrayList<>();
        for (SpectrumMatchMethod spectrumMatchMethod : SpectrumMatchMethod.values()) {
            header.add(spectrumMatchMethod.getName());
            methodDO.setSpectrumMatchMethod(spectrumMatchMethod);
            List<List<Object>> dataSheet = getDataSheet(querySpectrumDOS, targetLibraryId, null, methodDO, scoreInterval, 1);
            for (int j = 0; j < dataSheet.size(); j++) {
                //trueFDR
                Double trueFDR = (Double) dataSheet.get(j).get(7);
                compareSheetFDR.get(j).add(trueFDR);

                Double FPR = (Double) dataSheet.get(j).get(17);
                compareSheetFPR.get(j).add(FPR);

                Double TPR = (Double) dataSheet.get(j).get(18);
                compareSheetTPR.get(j).add(TPR);

                Double Pre = (Double) dataSheet.get(j).get(19);
                compareSheetPre.get(j).add(Pre);

                Double AUC = (Double) dataSheet.get(j).get(20);
                compareSheetAUC.get(j).add(AUC);
            }
        }
        compareSheetFDR.add(0, header);
        compareSheetFPR.add(0, header);
        compareSheetTPR.add(0, header);
        compareSheetPre.add(0, header);
        compareSheetAUC.add(0, header);

        EasyExcel.write(outputFileName_FDR).sheet(fileName_FDR).doWrite(compareSheetFDR);
        EasyExcel.write(outputFileName_FPR).sheet(fileName_FPR).doWrite(compareSheetFPR);
        EasyExcel.write(outputFileName_TPR).sheet(fileName_TPR).doWrite(compareSheetTPR);
        EasyExcel.write(outputFileName_Pre).sheet(fileName_Pre).doWrite(compareSheetPre);
        EasyExcel.write(outputFileName_AUC).sheet(fileName_AUC).doWrite(compareSheetAUC);

        log.info("export {} success", fileName_FDR);
        log.info("export {} success", fileName_FPR);
        log.info("export {} success", fileName_TPR);
        log.info("export {} success", fileName_Pre);
        log.info("export {} success", fileName_AUC);
    }


    private List<List<Object>> getDataSheet(List<SpectrumDO> querySpectrumDOS, String targetLibraryId, String decoyLibraryId, MethodDO methodDO, int scoreInterval, int decoyMultiplier) {
        ConcurrentHashMap<SpectrumDO, List<LibraryHit>> hitsMap = libraryHitService.getTargetDecoyHitsMap(querySpectrumDOS, targetLibraryId, decoyLibraryId, methodDO);
        List<List<Object>> dataSheet = new ArrayList<>();

        //all hits above a score threshold for the target-decoy strategy
        List<LibraryHit> decoyHits = new ArrayList<>();
        List<LibraryHit> targetHits = new ArrayList<>();

        //the top score hits of each spectrum
        List<LibraryHit> bestDecoyHits = new ArrayList<>();
        List<LibraryHit> bestTargetHits = new ArrayList<>();
        List<LibraryHit> ctdcList = new ArrayList<>();

        //for true FDR calculation and ROC curve calculation
        List<LibraryHit> truePositives = new ArrayList<>();
        List<LibraryHit> falsePositives = new ArrayList<>();
        List<LibraryHit> tureNegatives = new ArrayList<>();
        List<LibraryHit> falseNegatives = new ArrayList<>();

        //collect data
        hitsMap.forEach((k, v) -> {
            if (!v.isEmpty()) {
                //concatenated target-decoy competition
                v.sort(Comparator.comparing(LibraryHit::getScore).reversed());
                ctdcList.add(v.get(0));

                //separated target-decoy competition
                Map<Boolean, List<LibraryHit>> decoyTargetMap = v.stream().collect(Collectors.groupingBy(LibraryHit::isDecoy));
                List<LibraryHit> targetHitsList = decoyTargetMap.get(false);
                List<LibraryHit> decoyHitsList = decoyTargetMap.get(true);
                if (targetHitsList != null && !targetHitsList.isEmpty()) {
                    targetHitsList.sort(Comparator.comparing(LibraryHit::getScore).reversed());
                    bestTargetHits.add(targetHitsList.get(0));
                    for (LibraryHit hit : targetHitsList) {
                        String[] inChIKeyArray = hit.getInChIKey().split("-");
                        if (inChIKeyArray[0].equals(k.getInChIKey().split("-")[0])) {
                            truePositives.add(hit);
                            falseNegatives.add(hit);
                            hit.setRight(true);
                        } else {
                            falsePositives.add(hit);
                            tureNegatives.add(hit);
                        }
                    }
                    targetHits.addAll(targetHitsList);
                }
                if (decoyHitsList != null && !decoyHitsList.isEmpty()) {
                    decoyHitsList.sort(Comparator.comparing(LibraryHit::getScore).reversed());
                    bestDecoyHits.add(decoyHitsList.get(0));
                    decoyHits.addAll(decoyHitsList);
                }
            }
        });

        int thresholdTPCount,thresholdFPCount;
        thresholdTPCount = truePositives.stream().filter(hit -> hit.getScore() >= 0.8).toList().size();
        thresholdFPCount = falsePositives.stream().filter(hit -> hit.getScore() >= 0.8).toList().size();
        log.info("method:{}, thresholdTPCount:{}, thresholdFPCount:{}",methodDO.getSpectrumMatchMethod(),thresholdTPCount,thresholdFPCount);
        if ((thresholdTPCount+thresholdFPCount) != 0)
            log.info("Precision : {}", thresholdTPCount/(thresholdTPCount+thresholdFPCount));

        //AUC calculation
        double AUC = 0d, rightCount = 0d, falseCount = 0d;
        targetHits.sort(Comparator.comparing(LibraryHit::getScore));
        for (int i = 0; i < targetHits.size(); i++) {
            LibraryHit hit = targetHits.get(i);
            if (hit.isRight()) {
                rightCount++;
            } else {
                AUC += i;
                falseCount++;
            }
        }
        AUC = AUC - (rightCount * (rightCount + 1)) / 2;
        AUC = AUC / (rightCount * falseCount);

        //score range and step
        double minScore = 0.0;
        double maxScore = 1.0;
        double step = (maxScore - minScore) / scoreInterval;

        //estimate PIT
        double threshold = 0.5;
        double PIT = 0d;
        if (!decoyHits.isEmpty()) {
            PIT = (double) targetHits.stream().filter(hit -> hit.getScore() < threshold).toList().size() / decoyHits.stream().filter(hit -> hit.getScore() < threshold).toList().size() / decoyMultiplier;
        }

        for (int i = 0; i < scoreInterval; i++) {
            double finalMinScore = minScore + i * step;
            double finalMaxScore = minScore + (i + 1) * step;
            List<Object> row = new ArrayList<>();

            //concatenated target-decoy strategy calculation
            int target;
            double decoy;
            List<LibraryHit> hitsAboveScore = ctdcList.stream().filter(hit -> hit.getScore() > finalMinScore).toList();
            target = hitsAboveScore.stream().filter(hit -> !hit.isDecoy()).toList().size();
            decoy = (double) hitsAboveScore.stream().filter(LibraryHit::isDecoy).toList().size() / decoyMultiplier;
            double CTDC_FDR = (target + decoy == 0) ? 0d : (double) 2 * decoy / (target + decoy);
            double TTDC_FDR = (target == 0) ? 0d : decoy / target;

            //separated target-decoy strategy calculation
            int targetCount, bestTargetCount;
            int truePositiveCount, falsePositiveCount, trueNegativeCount, falseNegativeCount;
            double decoyCount, bestDecoyCount;
            targetCount = targetHits.stream().filter(hit -> hit.getScore() >= finalMinScore).toList().size();
            decoyCount = (double) decoyHits.stream().filter(hit -> hit.getScore() >= finalMinScore).toList().size() / decoyMultiplier;
            bestTargetCount = bestTargetHits.stream().filter(hit -> hit.getScore() >= finalMinScore).toList().size();
            bestDecoyCount = (double) bestDecoyHits.stream().filter(hit -> hit.getScore() >= finalMinScore).toList().size() / decoyMultiplier;

            //real data calculation
            truePositiveCount = truePositives.stream().filter(hit -> hit.getScore() >= finalMinScore).toList().size();
            falsePositiveCount = falsePositives.stream().filter(hit -> hit.getScore() >= finalMinScore).toList().size();
            trueNegativeCount = tureNegatives.stream().filter(hit -> hit.getScore() < finalMinScore).toList().size();
            falseNegativeCount = falseNegatives.stream().filter(hit -> hit.getScore() < finalMinScore).toList().size();
            double trueFDR = 0d, BestSTDS_FDR = 0d, STDS_FDR = 0d, pValue = 0d, TPR = 0d, FPR = 0d, ROC = 0d;

            if (truePositiveCount + falsePositiveCount != 0) {
                trueFDR = (double) falsePositiveCount / (truePositiveCount + falsePositiveCount);
            }

            if (bestTargetCount != 0) {
                BestSTDS_FDR = bestDecoyCount / (bestTargetCount + bestDecoyCount);
            }
            if (targetCount != 0) {
                STDS_FDR = decoyCount / (targetCount + decoyCount);
                pValue = decoyCount / (targetCount);
            }

            //hits distribution
            if (i == 0) {
                targetCount = targetHits.stream().filter(hit -> hit.getScore() >= finalMinScore && hit.getScore() <= finalMaxScore).toList().size();
                decoyCount = (double) decoyHits.stream().filter(hit -> hit.getScore() >= finalMinScore && hit.getScore() <= finalMaxScore).toList().size() / decoyMultiplier;
            } else {
                targetCount = targetHits.stream().filter(hit -> hit.getScore() > finalMinScore && hit.getScore() <= finalMaxScore).toList().size();
                decoyCount = (double) decoyHits.stream().filter(hit -> hit.getScore() > finalMinScore && hit.getScore() <= finalMaxScore).toList().size() / decoyMultiplier;
            }

            //write data sheet
            //start score
            row.add(finalMinScore);
            //end score
            row.add(finalMaxScore);
            //target frequency
            row.add((double) targetCount / targetHits.size());
            //decoy frequency
            row.add(decoyHits.size() == 0 ? 0 : decoyCount / decoyHits.size());
            //total frequency
            row.add((targetCount + decoyCount) / (targetHits.size() + decoyHits.size()));
            //CTDC FDR
            row.add(CTDC_FDR);
            //TTDC FDR
            row.add(TTDC_FDR);
            //trueFDR
            row.add(trueFDR);
            //BestSTDS FDR
            row.add(BestSTDS_FDR);
            //STDS FDR
            row.add(STDS_FDR);
            //standard FDR
            row.add(trueFDR);
            //pValue
            row.add(pValue);
            //PIT
            row.add(PIT);
            //true positive count
            row.add(truePositiveCount);
            //false positive count
            row.add(falsePositiveCount);
            //true negative count
            row.add(trueNegativeCount);
            //false negative count
            row.add(falseNegativeCount);
            //false positive rate  17
            // tn 预测结果为负样本，实际为负样本
            // tp 预测结果为正样本，实际也为正样本
            // FP 预测结果为正样本，实际为负样本
            // fn 预测结果为负样本，实际为正样本
            if (falsePositiveCount + trueNegativeCount != 0)
                row.add(falsePositiveCount / (double) (falsePositiveCount + trueNegativeCount));
            //FP+TN：真实负样本的总和，负样本被误识别为正样本数量+正确分类的负样本数量。
            //ture positive rate   18     TPR = Recall
            if (truePositiveCount + falseNegativeCount != 0)
                row.add(truePositiveCount / (double) (truePositiveCount + falseNegativeCount));
            //TP+FN 真实正样本的总和，正确分类的正样本数量+漏报的正样本数量。
            //precision  19
            if (truePositiveCount + falsePositiveCount != 0){
                row.add(truePositiveCount / (double) (falsePositiveCount + truePositiveCount));
            } else {
                row.add(1d);
            }
            //AUC 20
            row.add(AUC);
            dataSheet.add(row);
        }

        return dataSheet;
    }

    public void ionEntropyDistributionGraph(String libraryId) {
        String fileName = "ionEntropyDistributionGraph" + SymbolConst.DELIMITER + libraryId;
        String outputFileName = vmProperties.getRepository() + File.separator + fileName + ".xlsx";
        Double maxEntropy = 5d;

        List<List<Object>> dataSheet = new ArrayList<>();
        List<SpectrumDO> spectrumDOS = spectrumService.getAllByLibraryId(libraryId);

        //precursor based method
        List<IonPeak> precursorIonPeaks = Collections.synchronizedList(new ArrayList<>());
        List<IonPeak> ionPeaks = Collections.synchronizedList(new ArrayList<>());
        spectrumDOS.parallelStream().forEach(spectrumDO -> {
            Double mzTolerance = 10 * Constants.PPM * spectrumDO.getPrecursorMz();
            List<SpectrumDO> candidateSpectra = spectrumDOS.stream().filter(spectrumDO1 -> Math.abs(spectrumDO1.getPrecursorMz() - spectrumDO.getPrecursorMz()) < mzTolerance).toList();
            List<IonPeak> candidateIonPeaks = new ArrayList<>();
            for (SpectrumDO candidate : candidateSpectra) {
                int precursorIndex = ArrayUtil.findNearestIndex(candidate.getMzs(), spectrumDO.getPrecursorMz());
                double precursorIntensity = candidate.getInts()[precursorIndex];
                for (int j = 0; j < candidate.getMzs().length; j++) {
                    IonPeak candidateIonPeak = new IonPeak(candidate.getMzs()[j], candidate.getInts()[j] / precursorIntensity);
                    candidateIonPeaks.add(candidateIonPeak);
                }
            }
            for (int i = 0; i < spectrumDO.getMzs().length; i++) {
                int precursorIndex = ArrayUtil.findNearestIndex(spectrumDO.getMzs(), spectrumDO.getPrecursorMz());
                double precursorIntensity = spectrumDO.getInts()[precursorIndex];
                IonPeak ionPeak = new IonPeak(spectrumDO.getMzs()[i], spectrumDO.getInts()[i] / precursorIntensity);
                Double ionMzTolerance = 10 * Constants.PPM * ionPeak.getMz();
                List<IonPeak> targetIonPeaks = candidateIonPeaks.stream().filter(ionPeak1 -> Math.abs(ionPeak1.getMz() - ionPeak.getMz()) < ionMzTolerance).toList();
                if (targetIonPeaks.size() == 1) {
                    ionPeak.setIonEntropy(0d);
                } else {
                    ionPeak.setIonEntropy(Entropy.getIonEntropy(targetIonPeaks.stream().mapToDouble(IonPeak::getIntensity).toArray()));
                }
                ionPeaks.add(ionPeak);
            }
        });

        int ZeroCount = ionPeaks.stream().filter(ionPeak -> ionPeak.getIonEntropy() == 0d).toList().size();
        int MaxCount = ionPeaks.stream().filter(ionPeak -> ionPeak.getIonEntropy() >= 1d).toList().size();
        //这里看一看>=1的数量和==1的数量是否一致
        //ion entropy distribution graph
        int nonZeroIonCount = ionPeaks.size() - ZeroCount;
        for (int i = 0; i < 100; i++) {
            final double minIonEntropy = i / 100d * maxEntropy;
            final double maxIonEntropy = (i + 1) / 100d * maxEntropy;
            List<Object> row = new ArrayList<>();
            int ionCount = 0;
            for (IonPeak ionPeak : ionPeaks) {
                if (ionPeak.getIonEntropy() > minIonEntropy && ionPeak.getIonEntropy() <= maxIonEntropy) {
                    ionCount++;
                }
            }
            row.add(minIonEntropy);
            row.add(maxIonEntropy);
            row.add((double) ionCount / nonZeroIonCount);
            row.add(ZeroCount);
            row.add(MaxCount);
            dataSheet.add(row);
        }

        EasyExcel.write(outputFileName).sheet(fileName).doWrite(dataSheet);
        log.info("export {} success", fileName);

    }

}
