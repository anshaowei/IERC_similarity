package net.csibio.metaphoenix.core.dao;

import net.csibio.metaphoenix.client.domain.db.LibraryDO;
import net.csibio.metaphoenix.client.domain.query.LibraryQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class LibraryDAO extends BaseDAO<LibraryDO, LibraryQuery> {

    public static String CollectionName = "library";

    @Override
    protected String getCollectionName() {
        return CollectionName;
    }

    @Override
    protected Class getDomainClass() {
        return LibraryDO.class;
    }

    @Override
    protected boolean allowSort() {
        return true;
    }

    @Override
    protected Query buildQueryWithoutPage(LibraryQuery libraryQuery) {
        Query query = new Query();
        if (libraryQuery.getId() != null && !libraryQuery.getId().isEmpty()) {
            query.addCriteria(where("id").is(libraryQuery.getId()));
        } else if (libraryQuery.getIds() != null && libraryQuery.getIds().size() != 0) {
            query.addCriteria(where("id").in(libraryQuery.getIds()));
        }
        if (libraryQuery.getName() != null) {
            query.addCriteria(where("name").regex(libraryQuery.getName(), "i"));
        }
        if (libraryQuery.getType() != null) {
            query.addCriteria(where("type").is(libraryQuery.getType()));
        }
        if (libraryQuery.getPlatform() != null) {
            query.addCriteria(where("platform").is(libraryQuery.getPlatform()));
        }
        if (libraryQuery.getSpecies() != null) {
            query.addCriteria(where("species").in(libraryQuery.getSpecies()));
        }
        if (libraryQuery.getMatrix() != null) {
            query.addCriteria(where("matrix").in(libraryQuery.getMatrix()));
        }
        if (libraryQuery.getCreateDateStart() != null && libraryQuery.getCreateDateEnd() != null) {
            query.addCriteria(where("createDate").gte(libraryQuery.getCreateDateStart()).lt(libraryQuery.getCreateDateEnd()));
        }
        if (allowSort()) {
            if (libraryQuery.getSortColumn() != null && libraryQuery.getOrderBy() != null) {
                query.with(Sort.by(libraryQuery.getOrderBy(), libraryQuery.getSortColumn()));
            }
        }
        return query;
    }
}
