package net.csibio.metaphoenix.core.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.csibio.metaphoenix.client.domain.db.LibraryDO;
import net.csibio.metaphoenix.client.domain.query.LibraryQuery;
import net.csibio.metaphoenix.client.exceptions.XException;
import net.csibio.metaphoenix.client.service.IDAO;
import net.csibio.metaphoenix.client.service.LibraryService;
import net.csibio.metaphoenix.client.service.SpectrumService;
import net.csibio.metaphoenix.core.dao.LibraryDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service("libraryService")
public class LibraryServiceImpl implements LibraryService {

    @Autowired
    LibraryDAO libraryDAO;
    @Autowired
    SpectrumService spectrumService;

    @Override
    public long count(LibraryQuery query) {
        return libraryDAO.count(query);
    }

    @Override
    public IDAO<LibraryDO, LibraryQuery> getBaseDAO() {
        return libraryDAO;
    }

    @Override
    public void beforeInsert(LibraryDO library) throws XException {
        library.setCreateDate(new Date());
        library.setLastModifiedDate(new Date());
        library.setId(library.getName());
    }

}
