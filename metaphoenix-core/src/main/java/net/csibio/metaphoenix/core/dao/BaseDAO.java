package net.csibio.metaphoenix.core.dao;

import net.csibio.metaphoenix.client.domain.query.PageQuery;
import net.csibio.metaphoenix.client.service.IDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public abstract class BaseDAO<T, Q extends PageQuery> implements IDAO<T, Q> {

    @Autowired
    MongoTemplate mongoTemplate;

    protected abstract String getCollectionName();

    protected abstract Class<T> getDomainClass();

    protected abstract boolean allowSort();

    protected abstract Query buildQueryWithoutPage(Q query);


    @SuppressWarnings("Carefully Using!!!")
    public List<T> getAll(Q query) {
        return mongoTemplate.find(buildQueryWithoutPage(query), getDomainClass(), getCollectionName());
    }

    public long count(Q query) {
        return mongoTemplate.count(buildQueryWithoutPage(query), getDomainClass(), getCollectionName());
    }

    public T insert(T t) {
        mongoTemplate.insert(t, getCollectionName());
        return t;
    }

    public List<T> insert(List<T> list) {
        mongoTemplate.insert(list, getCollectionName());
        return list;
    }

}
