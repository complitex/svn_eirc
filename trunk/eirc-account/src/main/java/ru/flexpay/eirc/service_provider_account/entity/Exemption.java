package ru.flexpay.eirc.service_provider_account.entity;

import org.apache.commons.lang.builder.ToStringBuilder;
import ru.flexpay.eirc.dictionary.entity.DictionaryTemporalObject;

/**
 * @author Pavel Sknar
 */
public class Exemption extends DictionaryTemporalObject {
    private Long ownerExemptionId;
    private String category;
    private Integer numberUsing;

    public Long getOwnerExemptionId() {
        return ownerExemptionId;
    }

    public void setOwnerExemptionId(Long ownerExemptionId) {
        this.ownerExemptionId = ownerExemptionId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getNumberUsing() {
        return numberUsing;
    }

    public void setNumberUsing(Integer numberUsing) {
        this.numberUsing = numberUsing;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("ownerExemptionId", ownerExemptionId)
                .append("category", category)
                .append("numberUsing", numberUsing)
                .toString();
    }
}
