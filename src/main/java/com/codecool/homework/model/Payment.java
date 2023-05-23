package com.codecool.homework.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Objects;

@Getter
@Setter
public class Payment {
    // to keep the data from csv
    private final String webshopId;
    // to keep the data from csv
    private final String customerId;
    // webshop id and customer id concatenated
    private final String uniqueCustomerId;
    // card or transfer
    private String type;
    // BigInteger to keep the amount precisely and integer due to fillér as a currency is not in circulation,
    // so need for a floating-point number (decimal)
    private BigInteger amountPayed;
    private String bankAccountNumber;
    private String creditCardNumber;
    private String dateOfPayment;
    private Customer customer;

    public Payment(String webshopId, String customerId, String type, BigInteger amountPayed,
                   String bankAccountNumber, String creditCardNumber, String dateOfPayment, Customer customer) {
        this.webshopId = webshopId;
        this.customerId = customerId;
        this.uniqueCustomerId = webshopId + customerId;
        this.type = type;
        this.amountPayed = amountPayed;
        this.bankAccountNumber = bankAccountNumber;
        this.creditCardNumber = creditCardNumber;
        this.dateOfPayment = dateOfPayment;
        this.customer = customer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return Objects.equals(getUniqueCustomerId(), payment.getUniqueCustomerId()) && Objects.equals(getType(), payment.getType())
                && Objects.equals(getAmountPayed(), payment.getAmountPayed())
                && Objects.equals(getBankAccountNumber(), payment.getBankAccountNumber())
                && Objects.equals(getCreditCardNumber(), payment.getCreditCardNumber())
                && Objects.equals(getDateOfPayment(), payment.getDateOfPayment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUniqueCustomerId(), getType(), getAmountPayed(), getBankAccountNumber(), getCreditCardNumber(),
                getDateOfPayment());
    }
}
