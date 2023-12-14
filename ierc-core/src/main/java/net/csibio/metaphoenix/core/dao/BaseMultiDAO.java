package net.csibio.metaphoenix.core.dao;

import net.csibio.metaphoenix.client.domain.query.PageQuery;
import net.csibio.metaphoenix.client.service.IMultiDAO;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public abstract class BaseMultiDAO<T, Q extends PageQuery> implements IMultiDAO<T, Q> {


    @Autowired
    MongoTemplate mongoTemplate;

    protected abstract String getCollectionName(String routerId);

    protected abstract Class<T> getDomainClass();

    protected abstract Query buildQueryWithoutPage(Q query);

    @SuppressWarnings("Carefully Using!!!")
    public List<T> getAll(Q query, String routerId) {
        return mongoTemplate.find(buildQueryWithoutPage(query), getDomainClass(), getCollectionName(routerId));
    }

    public long count(Q query, String routerId) {
        return mongoTemplate.count(buildQueryWithoutPage(query), getDomainClass(), getCollectionName(routerId));
    }

    public T insert(T t, String routerId) {
        mongoTemplate.insert(t, getCollectionName(routerId));
        return t;
    }

    public List<T> insert(List<T> list, String routerId) {
        mongoTemplate.insert(list, getCollectionName(routerId));
        return list;
    }

    public void remove(Q q, String routerId) {
        mongoTemplate.remove(buildQueryWithoutPage(q), getDomainClass(), getCollectionName(routerId));
    }

    public void buildIndex(Class clazz, String routerId) {
        String collectionName = getCollectionName(routerId);
        IndexOperations indexOps = mongoTemplate.indexOps(collectionName);
        String[] indexFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Indexed.class))
                .map(Field::getName)
                .toArray(String[]::new);
        for (String indexField : indexFields) {
            if (StringUtils.hasText(indexField)) {
                indexOps.ensureIndex(new Index(indexField, Sort.Direction.ASC));
            }
        }
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().equals(CompoundIndexes.class)) {
                CompoundIndexes indexes = ((CompoundIndexes) clazz.getAnnotations()[1]);
                CompoundIndex[] indexesArray = indexes.value();
                if (indexesArray.length > 0) {
                    for (int i = 0; i < indexesArray.length; i++) {
                        CompoundIndex index = indexesArray[i];
                        Document document = Document.parse(index.def());
                        CompoundIndexDefinition definition = new CompoundIndexDefinition(document);
                        if (index.unique()) {
                            indexOps.ensureIndex(definition.unique());
                        } else {
                            indexOps.ensureIndex(definition);
                        }
                    }
                }

            }
        }
    }
}
