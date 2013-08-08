package ru.flexpay.eirc.dictionary.entity;

import org.complitex.address.entity.AddressEntity;

import java.io.Serializable;

/**
 * @author Pavel Sknar
 */
public class Address implements Serializable {

    private Long id;
    private AddressEntity entity;

    private String country;
    private String region;
    private String city;
    private String cityType;
    private String district;
    private String street;
    private String streetType;
    private String building;
    private String apartment;
    private String room;

    public Address() {
    }

    public Address(Long id, AddressEntity entity) {
        this.id = id;
        this.entity = entity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AddressEntity getEntity() {
        return entity;
    }

    public void setEntity(AddressEntity entity) {
        this.entity = entity;
    }

    public void setEntityId(long entityId) {
        for (AddressEntity addressEntity : AddressEntity.values()) {
            if (entityId == addressEntity.getId()) {
                entity = addressEntity;
                break;
            }
        }
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCityType() {
        return cityType;
    }

    public void setCityType(String cityType) {
        this.cityType = cityType;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getStreetType() {
        return streetType;
    }

    public void setStreetType(String streetType) {
        this.streetType = streetType;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getApartment() {
        return apartment;
    }

    public void setApartment(String apartment) {
        this.apartment = apartment;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}
