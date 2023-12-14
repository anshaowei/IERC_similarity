package net.csibio.metaphoenix.client.domain.bean.chemical;

import lombok.Data;


@Data
public class OpElement {

    // 是否是加运算,否则为减运算
    boolean plus;

    Double monoMw;

    String symbol;

    // 对应的数目, 默认为1
    Integer n = 1;

}
