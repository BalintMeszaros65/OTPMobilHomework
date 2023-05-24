package com.codecool.homework.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@EqualsAndHashCode
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
    // so no need for a floating-point number (decimal)
    private BigInteger amountPayed;
    private String bankAccountNumber;
    private String creditOrDebitCardNumber;
    private String dateOfPayment;
    private Customer customer;

    public Payment(String webshopId, String customerId, String type, BigInteger amountPayed,
                   String bankAccountNumber, String creditOrDebitCardNumber, String dateOfPayment, Customer customer) {
        this.webshopId = webshopId;
        this.customerId = customerId;
        this.uniqueCustomerId = webshopId + customerId;
        this.type = type;
        this.amountPayed = amountPayed;
        this.bankAccountNumber = bankAccountNumber;
        this.creditOrDebitCardNumber = creditOrDebitCardNumber;
        this.dateOfPayment = dateOfPayment;
        this.customer = customer;
    }
}
