package net.csibio.metaphoenix.client.service;

import net.csibio.metaphoenix.client.domain.query.PageQuery;

import java.util.List;

public interface IMultiDAO<T, Q extends PageQuery> {

    @SuppressWarnings("Carefully Using!!!")
    List<T> getAll(Q query, String routerId);

    long count(Q query, String routerId);

    T insert(T t, String routerId);

    List<T> insert(List<T> list, String routerId);

    void remove(Q query, String routerId);

    void buildIndex(Class clazz, String routerId);

}
