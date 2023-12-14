package net.csibio.metaphoenix.client.service;

import net.csibio.metaphoenix.client.constants.enums.ResultCode;
import net.csibio.metaphoenix.client.domain.Result;
import net.csibio.metaphoenix.client.domain.query.PageQuery;
import net.csibio.metaphoenix.client.exceptions.XException;

import java.util.List;

public interface BaseService<T, Q extends PageQuery> {

    default Result<T> insert(T t) {
        try {
            beforeInsert(t);
            getBaseDAO().insert(t);
            afterInsert(t);
            return Result.OK(t);
        } catch (XException xe) {
            return Result.Error(xe.getCode(), xe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.INSERT_ERROR);
        }
    }

    default Result<List<T>> insert(List<T> tList) {
        try {
            for (T t : tList) {
                beforeInsert(t);
            }
            List<T> successInsertList = getBaseDAO().insert(tList);
            for (T t : tList) {
                afterInsert(t);
            }
            return Result.OK(successInsertList);
        } catch (XException xe) {
            return Result.Error(xe.getCode(), xe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.INSERT_ERROR);
        }
    }

    default List<T> getAll(Q q) {
        return getBaseDAO().getAll(q);
    }

    default long count(Q q) {
        return getBaseDAO().count(q);
    }

    IDAO<T, Q> getBaseDAO();

    default void beforeInsert(T t) throws XException {
    }

    default void afterInsert(T t) throws XException {
    }
}
