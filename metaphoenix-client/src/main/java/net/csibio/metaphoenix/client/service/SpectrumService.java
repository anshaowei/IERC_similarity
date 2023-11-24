package net.csibio.metaphoenix.client.service;

import net.csibio.metaphoenix.client.domain.db.SpectrumDO;
import net.csibio.metaphoenix.client.domain.query.SpectrumQuery;

import java.util.List;

public interface SpectrumService extends BaseMultiService<SpectrumDO, SpectrumQuery> {

    List<SpectrumDO> getAllByLibraryId(String libraryId);

    List<SpectrumDO> getByPrecursorMz(Double precursorMz, Double mzTolerance, String libraryId);

}

