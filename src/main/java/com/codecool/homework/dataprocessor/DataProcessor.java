package com.codecool.homework.dataprocessor;

import com.codecool.homework.model.Customer;
import com.codecool.homework.model.Payment;
import com.codecool.homework.util.CsvFileHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * Entry point for the project.
 *
 * @author Bálint Mészáros
 */
@SpringBootApplication
public class DataProcessor implements CommandLineRunner {
    CsvFileHandler csvFileHandler;

    public DataProcessor(CsvFileHandler csvFileHandler) {
        this.csvFileHandler = csvFileHandler;
    }

    @Override
    public void run(String... args) throws Exception {
        // Logger setup
        // TODO suppress console log
        FileHandler handler = new FileHandler("application.log", true);
        Logger logger = Logger.getLogger("com.codecool.homework.dataprocessor");
        logger.addHandler(handler);
        // hardcoded customer and payments file
        // TODO refactor to use args as input
        File customerFile = new File("src/main/resources/input/customer.csv");
        File paymentsFile = new File("src/main/resources/input/payments.csv");
        // reading data from customer.csv and payments.csv and storing the 2d matrices
        // not catching FileNotFoundException because if the files are not found the program is redundant
        List<List<String>> rawDataOfCustomers = csvFileHandler.readCsvData(customerFile);
        List<List<String>> rawDataOfPayments = csvFileHandler.readCsvData(paymentsFile);
        // validating customer data
        Set<Customer> customers = validateCustomers(logger, rawDataOfCustomers);
        // validating payments data
        List<Payment> payments = validatePayments(logger, rawDataOfPayments, customers);
        // creating report for customer payment sum
        Set<String> reportForCustomerPaymentSum = createReportForSumPaymentOfCustomers(customers, payments);
        // writing report to report01.csv file
        csvFileHandler.writeCsvData(reportForCustomerPaymentSum, "report01.csv");
    }

    // **************************************************
    // Private, helper methods
    // **************************************************
    /**
     * Validates each customer from row data.
     *
     * @param logger Logger for corrupt data handling
     * @param rawDataOfCustomers raw data of customers to consume
     *
     * @return set of Customers.
     *
     * @author Bálint Mészáros
     */
    private Set<Customer> validateCustomers(Logger logger, List<List<String>> rawDataOfCustomers) {
        Set<Customer> customers = new HashSet<>();
        Set<String> duplicateIds = collectDuplicateCustomerIds(rawDataOfCustomers);
        // validating each customer
        for (List<String> row : rawDataOfCustomers) {
            Optional<Customer> optionalCustomer = validateCustomer(row, duplicateIds, logger);
            optionalCustomer.ifPresent(customers::add);
        }
        return customers;
    }

    /**
     * Collects duplicate customer ids.
     *
     * @param rawDataOfCustomers raw data of customers to consume
     *
     * @return set of Strings.
     *
     * @author Bálint Mészáros
     */
    private static Set<String> collectDuplicateCustomerIds(List<List<String>> rawDataOfCustomers) {
        Set<String> uniqueCustomerIds = new HashSet<>();
        Set<String> duplicateIds = new HashSet<>();
        // checking if there are duplicate entries with same id
        for (List<String> row : rawDataOfCustomers) {
            String webshopId = row.get(0);
            String customerId = row.get(1);
            String uniqueCustomerId = webshopId + customerId;
            if (!uniqueCustomerIds.contains(uniqueCustomerId)) {
                uniqueCustomerIds.add(uniqueCustomerId);
            } else {
                duplicateIds.add(uniqueCustomerId);
            }
        }
        return duplicateIds;
    }

    /**
     * Validates one customer.
     * Logs the invalid data into application.log file.
     *
     * @param rawCustomer raw data of customer
     * @param duplicateIds duplicate unique (webshop + customer) ids of Customers
     * @param logger Logger for corrupt data handling
     *
     * @return empty optional or optional of Customer, depending on validation results.
     *
     * @author Bálint Mészáros
     */
    private Optional<Customer> validateCustomer(List<String> rawCustomer, Set<String> duplicateIds, Logger logger) {
        Optional<Customer> emptyCustomer = Optional.empty();

        String webshopId = rawCustomer.get(0);
        String id = rawCustomer.get(1);
        String name = rawCustomer.get(2);
        String address = rawCustomer.get(3);

        if (rawCustomer.size() != 4) {
            logger.severe("Invalid number of data in customer: " + String.join(";", rawCustomer));
            return emptyCustomer;
        }
        if (webshopId.length() != 4) {
            logger.severe("Invalid webshop id in customer: " + String.join(";", rawCustomer));
            return emptyCustomer;
        }
        // TODO add WS## check
        if (id.length() != 3) {
            logger.severe("Invalid id in customer: " + String.join(";", rawCustomer));
            return emptyCustomer;
        }
        if (duplicateIds.contains(id)) {
            logger.severe("Duplicate id found in customer: " + String.join(";", rawCustomer));
            return emptyCustomer;
        }
        // TODO add A## check
        if (name.isEmpty()) {
            logger.severe("Name is empty in customer: " + String.join(";", rawCustomer));
            return emptyCustomer;
        }
        if (address.isEmpty()) {
            logger.severe("Address is empty in customer: " + String.join(";", rawCustomer));
            return emptyCustomer;
        }

        // if all validation passes, creates Customer object
        Customer customer = new Customer(webshopId, id, name, address);
        return Optional.of(customer);
    }

    /**
     * Validates each payment from row data.
     *
     * @param logger Logger for corrupt data handling
     * @param rawDataOfPayments raw data of payments to consume
     * @param customers validated Customers
     *
     * @return list of Payments.
     *
     * @author Bálint Mészáros
     */
    // List due to the fact that there can be duplicate entries,
    // if the customer did the exact same payment multiple times at the same day
    // (not storing time of payment, just date)
    private List<Payment> validatePayments(Logger logger, List<List<String>> rawDataOfPayments, Set<Customer> customers) {
        List<Payment> payments = new ArrayList<>();
        for (List<String> row : rawDataOfPayments) {
            Optional<Payment> optionalPayment = validatePayment(customers, row, logger);
            optionalPayment.ifPresent(payments::add);
        }
        return payments;
    }

    /**
     * Validates one payment.
     * Logs the invalid data into application.log file.
     *
     * @param customers validated Customers
     * @param rawPayment raw data of payment
     * @param logger Logger for corrupt data handling
     *
     * @return empty optional or optional of Payment, depending on validation results.
     *
     * @author Bálint Mészáros
     */
    private Optional<Payment> validatePayment(Set<Customer> customers, List<String> rawPayment, Logger logger) {
        Optional<Payment> emptyPayment = Optional.empty();
        String rawPaymentString = String.join(";", rawPayment);

        // data from raw payment
        String webshopId = rawPayment.get(0);
        String customerId = rawPayment.get(1);
        String uniqueId = webshopId + customerId;
        String type = rawPayment.get(2);
        BigInteger amountPayed = null;
        try {
            amountPayed = BigInteger.valueOf(Integer.parseInt(rawPayment.get(3)));
        } catch (NumberFormatException ignore) {}
        String bankAccountNumber = rawPayment.get(4);
        String creditOrDebitCardNumber = rawPayment.get(5);
        String dateOfPayment = rawPayment.get(6);
        // find customer if exists
        Optional<Customer> optionalCustomer = customers
                .stream()
                .filter(customer -> customer.getUniqueId().equals(uniqueId))
                .findFirst();
        Customer customer = optionalCustomer.orElse(null);

        // validation logic
        if (rawPayment.size() != 7) {
            logger.severe("Invalid number of data in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (webshopId.length() != 4) {
            logger.severe("Invalid webshop id in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (customerId.length() != 3) {
            logger.severe("Invalid customer id in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (!type.equals("card") && !type.equals("transfer")) {
            logger.severe("Invalid type in payment: " + rawPaymentString);
            return emptyPayment;
        }
        // NumberFormatException case
        if (amountPayed == null) {
            logger.severe("Invalid amount format in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (amountPayed.compareTo(BigInteger.ZERO) == 0) {
            logger.warning("Amount is 0 in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (type.equals("card") && !bankAccountNumber.isEmpty()) {
            logger.severe("Card payment with bank account number found in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (type.equals("transfer") && !creditOrDebitCardNumber.isEmpty()) {
            logger.severe("Transfer payment with credit/debit card number found in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (!bankAccountNumber.isEmpty() && !creditOrDebitCardNumber.isEmpty()) {
            logger.severe("Both bank account number and credit/debit card number found in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (bankAccountNumber.isEmpty() && creditOrDebitCardNumber.isEmpty()) {
            logger.severe("Neither bank account number nor credit/debit card number found in payment: " + rawPaymentString);
            return emptyPayment;
        }
        // TODO might need better approach
        if (type.equals("transfer") && bankAccountNumber.length() < 8 || bankAccountNumber.length() > 24) {
            logger.severe("Bank account number length not valid in payment: " + rawPaymentString);
            return emptyPayment;
        }
        // American Express edge case
        if (type.equals("card") && creditOrDebitCardNumber.length() == 15
                && (!creditOrDebitCardNumber.startsWith("37") && !creditOrDebitCardNumber.startsWith("34"))) {
            logger.severe("Credit/debit card number is 15 long but not a valid American Express card: " + rawPaymentString);
            return emptyPayment;
        }
        if (type.equals("card") && (creditOrDebitCardNumber.length() < 15 || creditOrDebitCardNumber.length() > 19)) {
            logger.severe("Credit/debit card number length is not valid in payment: " + rawPaymentString);
            return emptyPayment;
        }
        // TODO check if credit card number is valid (Luhn algorithm check, etc.)
        if (dateOfPayment.length() != 10) {
            logger.severe("Date length is not valid in payment: " + rawPaymentString);
            return emptyPayment;
        }
        // TODO checking if the date is a real date
        if (customer == null) {
            logger.warning("Customer not found for payment: " + rawPaymentString);
            return emptyPayment;
        }

        // if all validation passes, creates Payment object
        Payment payment = new Payment(webshopId, customerId, type, amountPayed, bankAccountNumber, creditOrDebitCardNumber,
                dateOfPayment, customer);
        return Optional.of(payment);
    }

    /**
     * Creates a report for each Customer's sum of payment.
     *
     * @return set of String.
     *
     * @author Bálint Mészáros
     */
    private Set<String> createReportForSumPaymentOfCustomers(Set<Customer> customers, List<Payment> payments) {
        Set<String> report = new HashSet<>();
        for (Customer customer: customers) {
            StringBuilder stringBuilder = new StringBuilder();
            BigInteger sumOfPayment = payments.stream()
                    .filter(payment -> payment.getCustomer().equals(customer))
                    .map(Payment::getAmountPayed)
                    .reduce(BigInteger.ZERO, BigInteger::add);
            stringBuilder.append(customer.getName()).append(";")
                    .append(customer.getAddress()).append(";")
                    .append(sumOfPayment);
            report.add(stringBuilder.toString());
        }
        return report;
    }
}
