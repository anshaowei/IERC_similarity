package net.csibio.metaphoenix.client.service;

import net.csibio.metaphoenix.client.constants.enums.ResultCode;
import net.csibio.metaphoenix.client.domain.Result;
import net.csibio.metaphoenix.client.domain.query.PageQuery;
import net.csibio.metaphoenix.client.exceptions.XException;

import java.util.List;

public interface BaseMultiService<T, Q extends PageQuery> {

    default Result remove(Q q, String routerId) {
        try {
            getBaseDAO().remove(q, routerId);
            return Result.OK();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.DELETE_ERROR);
        }
    }
    default Result<T> insert(T t, String routerId) {
        try {
            beforeInsert(t, routerId);
            getBaseDAO().insert(t, routerId);
            return Result.OK(t);
        } catch (XException xe) {
            xe.printStackTrace();
            return Result.Error(xe.getCode(), xe.getMessage());
        } catch (Exception e) {
            return Result.Error(ResultCode.INSERT_ERROR);
        }
    }

    default Result<List<T>> insert(List<T> tList, String routerId) {
        try {
            for (T t : tList) {
                beforeInsert(t, routerId);
            }
            getBaseDAO().insert(tList, routerId);
            return Result.OK(tList);
        } catch (XException xe) {
            xe.printStackTrace();
            return Result.Error(xe.getCode(), xe.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.Error(ResultCode.INSERT_ERROR);
        }
    }

    default List<T> getAll(Q q, String routerId) {
        return getBaseDAO().getAll(q, routerId);
    }

    default long count(Q q, String routerId) {
        return getBaseDAO().count(q, routerId);
    }

    IMultiDAO<T, Q> getBaseDAO();

    void beforeInsert(T t, String routerId) throws XException;

}
