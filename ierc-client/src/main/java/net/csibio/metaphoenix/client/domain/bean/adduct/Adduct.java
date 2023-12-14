package net.csibio.metaphoenix.client.domain.bean.adduct;

import lombok.Data;
import net.csibio.metaphoenix.client.domain.bean.chemical.OpElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 注意:注释中的加和物是指 加和物本身,
 * 例如2M+H, H为加和物, 2M+H叫做加和靶标
 */
@Data
public class Adduct {

    /**
     * 加和形式,例如 4M+H+Na,,本字段最终由linkElements决定
     */
    String ionForm;

    List<OpElement> linkElements = new ArrayList<>();

    /**
     * 预测加和离子本身的分子质量, adduct_mass
     * 如果加合物是减去一个原子或者多个基团,则本字段为负,本字段最终由linkElements决定
     */
    Double mw;

    /**
     * 加和靶标
     */
    Integer charge;

    /**
     * 多聚物的数目
     */
    Integer n;

    /**
     * 理论出现的可能性百分比,单位为 %
     */
    Double probability;

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof Adduct) {
            Adduct other = (Adduct) obj;
            if (other.getIonForm() == null || this.getIonForm() == null) {
                return false;
            }
            if (this.getIonForm().equals(other.getIonForm())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return ionForm.hashCode();
    }

}
