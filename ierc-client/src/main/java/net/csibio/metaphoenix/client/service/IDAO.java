package net.csibio.metaphoenix.client.service;

import net.csibio.metaphoenix.client.domain.query.PageQuery;

import java.util.List;

public interface IDAO<T, Q extends PageQuery> {

    @SuppressWarnings("Carefully Using!!!")
    List<T> getAll(Q query);

    long count(Q query);

    T insert(T t);

    List<T> insert(List<T> list);

}
