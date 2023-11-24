package net.csibio.metaphoenix.client.domain.db;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Document("library")
public class LibraryDO implements Serializable {

    private static final long serialVersionUID = -3259329283915356627L;

    @Id
    String id;

    /**
     * 库名称
     */
    @Indexed(unique = true)
    String name;

    /**
     * 靶标建库来自的基质,可以是standard,plasma,urine等,也可以是多种基质的组合 @see Matrix
     */
    @Indexed
    Set<String> matrix = new HashSet<>();

    /**
     * 物种,例如HUMAN,也可以是多种物种的组合
     */
    @Indexed
    Set<String> species = new HashSet<>();

    Set<String> tags = new HashSet<>();

    Set<String> labels = new HashSet<>();

    /**
     * 库中条目的总数目
     */
    Integer count = 0;

    /**
     * 库中含有的谱图的数量
     */
    Integer spectrumCount = 0;

    /**
     * 库中含有的靶标的数量
     */
    String description;

    Date createDate;

    Date lastModifiedDate;
}
