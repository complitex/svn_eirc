package ru.flexpay.eirc.registry.entity;

import java.io.Serializable;
import java.util.Map;

/**
 * 
 * @author Artem
 */
public class ImportErrorDetail implements Serializable {

    private String id;
    private Long count;
    private Map<String, String> details;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public String getDetail(String detailName) {
        return details.get(detailName);
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
