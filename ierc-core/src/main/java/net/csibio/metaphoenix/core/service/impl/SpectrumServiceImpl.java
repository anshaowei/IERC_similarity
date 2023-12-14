package net.csibio.metaphoenix.core.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.metaphoenix.client.domain.db.SpectrumDO;
import net.csibio.metaphoenix.client.domain.query.SpectrumQuery;
import net.csibio.metaphoenix.client.exceptions.XException;
import net.csibio.metaphoenix.client.service.IMultiDAO;
import net.csibio.metaphoenix.client.service.SpectrumService;
import net.csibio.metaphoenix.core.dao.SpectrumDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("spectrumService")
public class SpectrumServiceImpl implements SpectrumService {

    public final Logger logger = LoggerFactory.getLogger(SpectrumServiceImpl.class);

    @Autowired
    SpectrumDAO spectrumDAO;

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public long count(SpectrumQuery query, String libraryId) {
        return spectrumDAO.count(query, libraryId);
    }

    @Override
    public IMultiDAO<SpectrumDO, SpectrumQuery> getBaseDAO() {
        return spectrumDAO;
    }

    @Override
    public void beforeInsert(SpectrumDO spectrum, String routerId) throws XException {
    }

    @Override
    public List<SpectrumDO> getAllByLibraryId(String libraryId) {
        try {
            return spectrumDAO.getAllByLibraryId(libraryId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    @Override
    public List<SpectrumDO> getByPrecursorMz(Double precursorMz, Double mzTolerance, String libraryId) {
        return spectrumDAO.getByPrecursorMz(precursorMz, mzTolerance, libraryId);
    }

}
