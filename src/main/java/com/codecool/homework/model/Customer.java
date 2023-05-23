package com.codecool.homework.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Customer {
    // to keep the data from csv
    private final String webshopId;
    // to keep the data from csv
    private final String id;
    // webshop id and customer id concatenated
    private final String uniqueId;
    private String name;
    private String address;

    public Customer(String webshopId, String id, String name, String address) {
        this.webshopId = webshopId;
        this.id = id;
        this.uniqueId = webshopId + id;
        this.name = name;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer customer)) return false;
        return Objects.equals(getUniqueId(), customer.getUniqueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUniqueId());
    }
}
