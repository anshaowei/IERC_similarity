package net.csibio.metaphoenix.client.domain.query;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class PageQuery implements Serializable {

    private static final long serialVersionUID = -8745138167696978267L;

    public static final int DEFAULT_PAGE_SIZE = 40;
    public static final String DEFAULT_SORT_COLUMN = "createDate";

    protected long current = 1;
    protected int pageSize = DEFAULT_PAGE_SIZE;
    protected long start = 0;
    //Sort.Direction.DESC
    protected Sort.Direction orderBy = null;
    protected String sortColumn = null;
    protected long total = 0;
    protected String sorter;
    protected long totalPage = 0;
    //是否使用estimateCount, 默认为false,即使用正常的count方法
    protected Boolean estimateCount = false;

    protected PageQuery() {
    }

    public Sort.Direction getOrderBy() {
        return orderBy;
    }

    public String getSortColumn() {
        return sortColumn;
    }

}
