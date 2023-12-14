package net.csibio.metaphoenix.client.domain.query;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class LibraryQuery extends PageQuery {

    String id;

    String name;

    String type;

    String platform;

    List<String> matrix;

    List<String> species;

    List<String> ids;

    Date createDateStart;

    Date createDateEnd;

    public LibraryQuery() {}
}
