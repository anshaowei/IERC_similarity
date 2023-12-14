package net.csibio.metaphoenix.core.controller;

import lombok.extern.slf4j.Slf4j;
import net.csibio.metaphoenix.client.constants.enums.SpectrumMatchMethod;
import net.csibio.metaphoenix.client.domain.db.LibraryDO;
import net.csibio.metaphoenix.client.domain.db.MethodDO;
import net.csibio.metaphoenix.client.domain.db.SpectrumDO;
import net.csibio.metaphoenix.client.domain.query.LibraryQuery;
import net.csibio.metaphoenix.client.filter.NoiseFilter;
import net.csibio.metaphoenix.client.parser.gnps.GnpsParser;
import net.csibio.metaphoenix.client.parser.massbank.MassBankParser;
import net.csibio.metaphoenix.client.service.LibraryService;
import net.csibio.metaphoenix.client.service.SpectrumService;
import net.csibio.metaphoenix.core.export.Reporter;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("test")
@Slf4j
public class TestController {

    @Autowired
    SpectrumService spectrumService;
    @Autowired
    LibraryService libraryService;
    @Autowired
    MassBankParser massBankParser;
    @Autowired
    Reporter reporter;
    @Autowired
    NoiseFilter noiseFilter;
    @Autowired
    GnpsParser gnpsParser;
    @Autowired
    MongoTemplate mongoTemplate;

    @RequestMapping("/importLibrary")
    public void importLibrary() {
        //gnps
        gnpsParser.parseMsp("E:\\MS_data\\MetaPhoenix\\library\\GNPS-NIST14-MATCHES.msp");
        gnpsParser.parseMsp("E:\\MS_data\\MetaPhoenix\\library\\ALL_GNPS.msp");
//
//        massbank
        massBankParser.parseMspEU("E:\\MS_data\\MetaPhoenix\\library\\MassBank_NIST.msp");
        massBankParser.parseMspMoNA("E:\\MS_data\\MetaPhoenix\\library\\MoNA-export-LC-MS-MS_Spectra.msp");
    }

    @RequestMapping("/filter")
    public void filter() {
        //filter all the libraries
        List<LibraryDO> libraryDOS = libraryService.getAll(new LibraryQuery());
        libraryDOS.parallelStream().forEach(libraryDO -> noiseFilter.filter(libraryDO.getId()));

        //basic filter
//        List<LibraryDO> libraryDOS = libraryService.getAll(new LibraryQuery());
//        libraryDOS.parallelStream().forEach(libraryDO -> noiseFilter.basicFilter(libraryDO.getId()));

        //filter on one library
//        String libraryId = "ST001794";
//        noiseFilter.easyfilter(libraryId);

    }

    @RequestMapping("dataExchange")
    public void dataExchange() throws IOException, InvalidFormatException {
        //real data
        File file = new File("E:\\MS_data\\MetaPhoenix\\library\\ST001794\\ST001794.xlsx");
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(4);   //获取SuppTable4信息
        List<SpectrumDO> spectrumDOS = new ArrayList<>();
        for (int i = 1; i < sheet.getLastRowNum(); i++) {
            if (i == 1 || i == 831 || i == 54) {
                continue;
            }
            Row row = sheet.getRow(i);
            if (row.getCell(1).getStringCellValue().contains("lvl 2A") || row.getCell(1).getStringCellValue().contains("lvl 1")) {
                SpectrumDO spectrumDO = new SpectrumDO();
                spectrumDO.setLibraryId("ST001794");
                for (Cell cell : row) {
                    switch (cell.getColumnIndex()) {
                        case 0 -> spectrumDO.setCompoundName(cell.getStringCellValue());
                        case 3 -> spectrumDO.setPrecursorMz(cell.getNumericCellValue());
                        case 9 -> spectrumDO.setInChIKey(cell.getStringCellValue());
                        case 12 -> {
                            String values = cell.getStringCellValue();
                            String[] valueArray = values.split(" ");
                            double[] mzArray = new double[valueArray.length];
                            double[] intensityArray = new double[valueArray.length];
                            for (int j = 0; j < valueArray.length; j++) {
                                String[] mzAndIntensity = valueArray[j].split(":");
                                mzArray[j] = Double.parseDouble(mzAndIntensity[0]);
                                intensityArray[j] = Double.parseDouble(mzAndIntensity[1]);
                            }
                            spectrumDO.setMzs(mzArray);
                            spectrumDO.setInts(intensityArray);
                        }
                    }
                }
                spectrumDOS.add(spectrumDO);
            }
        }
        spectrumService.insert(spectrumDOS, "ST001794");
        log.info("import success");
    }


    @RequestMapping("report")
    public void report() {
        //real score distribution sheet by the target-decoy strategy
        String queryLibraryId = "MassBank-MoNA";
        String targetLibraryId = "ALL_GNPS";
//        String queryLibraryId = "GNPS-NIST14-MATCHES";//MassBank-Europe GNPS-NIST14-MATCHES
//        String targetLibraryId = "MassBank-MoNA";
//          String queryLibraryId = "ST001794";
//          String targetLibraryId = "ALL_GNPS";
        MethodDO methodDO = new MethodDO();
        methodDO.setPpmForMzTolerance(true);
        methodDO.setPpm(10);
//        methodDO.setSpectrumMatchMethod(SpectrumMatchMethod.IonEntropyRankCosineSimilarity);
//        reporter.scoreGraph(queryLibraryId, targetLibraryId, decoyLibraryId, methodDO, 100);
        List<SpectrumDO> querySpectrumDOS = spectrumService.getAllByLibraryId(queryLibraryId);
        reporter.compareSpectrumMatchMethods(querySpectrumDOS, targetLibraryId, methodDO, 100);

    }

    @RequestMapping("compare")
    public void compare() {
        MethodDO methodDO = new MethodDO();
        methodDO.setPpmForMzTolerance(true);
        methodDO.setPpm(10);
        methodDO.setSpectrumMatchMethod(SpectrumMatchMethod.Entropy);
        String queryLibraryId = "MassBank-MoNA";
        String targetLibraryId = "ALL_GNPS";

        reporter.ionEntropyDistributionGraph(targetLibraryId);
    }

    @RequestMapping("all")
    public void all() {
        importLibrary();
        filter();
    }

}
