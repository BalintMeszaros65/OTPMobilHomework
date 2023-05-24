package com.codecool.homework.dataprocessor;

import com.codecool.homework.model.Customer;
import com.codecool.homework.model.Payment;
import com.codecool.homework.util.CsvFileHandler;
import com.codecool.homework.util.DateValidatorForNotIsoFormat;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Entry point for the project.
 *
 * @author Bálint Mészáros
 */
@SpringBootApplication
@AllArgsConstructor
public class DataProcessor implements CommandLineRunner {
    private CsvFileHandler csvFileHandler;
    private DateValidatorForNotIsoFormat dateValidator;

    @Override
    public void run(String... args) throws Exception {
        // Logger setup
        FileHandler handler = new FileHandler("application.log", true);
        // for file and console logging of invalid data read from input files
        Logger logger = Logger.getLogger("com.codecool.homework.dataprocessor");
        // for console logging the steps of the application
        Logger consoleLogger = Logger.getLogger("com.codecool.homework.console.logger");
        logger.addHandler(handler);

        /* check for csv files in arguments
        command to run as intended:
        mvn spring-boot:run -Dspring-boot.run.arguments="src/main/resources/input/customer.csv src/main/resources/input/payments.csv"
        */
        if (args.length < 2) {
            consoleLogger.severe("""
                    Missing customer.csv and/or payments.csv file paths as arguments.
                    Please start the program with the following arguments:
                    First - path to customer.csv
                    Second - path to payments.csv
                    Check documentation or README for further information.
                    Program shutting down""");
            System.exit(0);
        }
        String customerCsvFilePath = args[0];
        String paymentsCsvFilePath = args[1];
        File customerFile = new File(customerCsvFilePath);
        consoleLogger.info(String.format("File %s successfully opened.", customerCsvFilePath));
        File paymentsFile = new File(paymentsCsvFilePath);
        consoleLogger.info(String.format("File %s successfully opened.", paymentsCsvFilePath));
        // reading data from customer.csv and payments.csv and storing the 2d matrices
        // not catching FileNotFoundException because if the files are not found the program is redundant
        List<List<String>> rawDataOfCustomers = csvFileHandler.readCsvData(customerFile);
        consoleLogger.info("Data from \"customer.csv\" successfully read.");
        List<List<String>> rawDataOfPayments = csvFileHandler.readCsvData(paymentsFile);
        consoleLogger.info("Data from \"payments.csv\" successfully read.");
        // validating customer and payments data
        Set<Customer> customers = validateCustomers(logger, rawDataOfCustomers);
        consoleLogger.info("Customers data has been validated.");
        List<Payment> payments = validatePayments(logger, rawDataOfPayments, customers);
        consoleLogger.info("Payments data has been validated.");
        consoleLogger.info("Invalid data (if any) has been logged to application.log file.");
        // creating report of customer payment sum and writing it to report01.csv file
        Set<String> customersBySumPayment = createReportOfCustomersBySumPayment(customers, payments);
        csvFileHandler.writeCsvData(customersBySumPayment, "report01.csv");
        consoleLogger.info("Report \"report01.csv\" successfully created.");
        // creating top2 report from customer payment sum report writing it to top.csv file
        List<String> top2CustomerByPaymentSum = createReportOfTop2CustomerByPaymentSum(customersBySumPayment);
        csvFileHandler.writeCsvData(top2CustomerByPaymentSum, "top.csv");
        consoleLogger.info("Report \"top.csv\" successfully created.");
        // creating report of webshops by different payment sums and writing it to report02.csv file
        Set<String> webshopsByPaymentSums = createReportOfWebshopsByPaymentSums(payments);
        csvFileHandler.writeCsvData(webshopsByPaymentSums , "report02.csv");
        consoleLogger.info("Report \"report02.csv\" successfully created.");
        consoleLogger.info("Program finished running.");
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
        String rawCustomerString = String.join(";", rawCustomer);

        // data from raw Customer
        String webshopId = rawCustomer.get(0);
        String id = rawCustomer.get(1);
        String name = rawCustomer.get(2);
        String address = rawCustomer.get(3);

        // validation logic
        if (rawCustomer.size() != 4) {
            logger.severe("Invalid number of data in customer: " + rawCustomerString);
            return emptyCustomer;
        }
        if (webshopId.length() != 4) {
            logger.severe("Invalid webshop id in customer: " + rawCustomerString);
            return emptyCustomer;
        }
        if (!webshopId.startsWith("WS") || !Character.isDigit(webshopId.charAt(2))
                || !Character.isDigit(webshopId.charAt(3))) {
            logger.severe("Not valid (WS##) webshop id format in customer: " + rawCustomerString);
            return emptyCustomer;
        }
        if (id.length() != 3) {
            logger.severe("Invalid id in customer: " + rawCustomerString);
            return emptyCustomer;
        }
        if (!id.startsWith("A") || !Character.isDigit(id.charAt(1)) || !Character.isDigit(id.charAt(2))) {
            logger.severe("Not valid (A##) id format in customer: " + rawCustomerString);
            return emptyCustomer;
        }
        if (duplicateIds.contains(id)) {
            logger.severe("Duplicate id found in customer: " + rawCustomerString);
            return emptyCustomer;
        }
        if (name.isEmpty()) {
            logger.severe("Name is empty in customer: " + rawCustomerString);
            return emptyCustomer;
        }
        if (address.isEmpty()) {
            logger.severe("Address is empty in customer: " + rawCustomerString);
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
    /* List due to the fact that there can be duplicate entries,
    if the customer did the exact same payment multiple times at the same day
    (not storing time of payment, just the date) */
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
            amountPayed = new BigInteger(rawPayment.get(3));
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
        if (!webshopId.startsWith("WS") || !Character.isDigit(webshopId.charAt(2))
                || !Character.isDigit(webshopId.charAt(3))) {
            logger.severe("Not valid (WS##) webshop id format in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (customerId.length() != 3) {
            logger.severe("Invalid customer id in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (!customerId.startsWith("A") || !Character.isDigit(customerId.charAt(1))
                || !Character.isDigit(customerId.charAt(2))) {
            logger.severe("Not valid (A##) customer id format in payment: " + rawPaymentString);
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
        if (!bankAccountNumber.isEmpty() && !creditOrDebitCardNumber.isEmpty()) {
            logger.severe("Both bank account number and credit/debit card number found in payment: " + rawPaymentString);
            return emptyPayment;
        }
        if (bankAccountNumber.isEmpty() && creditOrDebitCardNumber.isEmpty()) {
            logger.severe("Neither bank account number nor credit/debit card number found in payment: " + rawPaymentString);
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
        if (!dateValidator.isValid(dateOfPayment)) {
            logger.severe("Date is not valid in payment: " + rawPaymentString);
            return emptyPayment;
        }
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
     * @param customers set of validated Customers
     * @param payments list of validated Payments
     *
     * @return set of String.
     *
     * @author Bálint Mészáros
     */
    private Set<String> createReportOfCustomersBySumPayment(Set<Customer> customers, List<Payment> payments) {
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

    /**
     * Creates a report for top 2 Customers by sum of payment.
     *
     * @param reportOfCustomersBySumPayment set of String containing the report created
     *                                      by createReportOfCustomersBySumPayment method
     *
     * @return list of String.
     *
     * @author Bálint Mészáros
     */
    private List<String> createReportOfTop2CustomerByPaymentSum(Set<String> reportOfCustomersBySumPayment) {
        List<String> listOfReportForCustomerPaymentSum = new ArrayList<>(reportOfCustomersBySumPayment);
        Comparator<String> comparator = Comparator.comparing(string -> string.split(";"),
                Comparator.comparing((String[] array) -> new BigInteger(array[2])).reversed());
        listOfReportForCustomerPaymentSum.sort(comparator);
        if (listOfReportForCustomerPaymentSum.size() > 2) {
            return listOfReportForCustomerPaymentSum.subList(0, 2);
        }
        return listOfReportForCustomerPaymentSum;
    }

    /**
     * Creates a report for each webshop's sum of payment by card and transfer.
     *
     * @param payments list of validated Payments
     *
     * @return set of String.
     *
     * @author Bálint Mészáros
     */
    private Set<String> createReportOfWebshopsByPaymentSums(List<Payment> payments) {
        Set<String> webshopIds = payments.stream()
                .map(Payment::getWebshopId)
                .collect(Collectors.toSet());
        Set<String> report = new HashSet<>();
        for (String webshopId: webshopIds) {
            StringBuilder stringBuilder = new StringBuilder();
            BigInteger cardPaymentsSum = payments.stream()
                    .filter(payment -> payment.getWebshopId().equals(webshopId))
                    .filter(payment -> payment.getType().equals("card"))
                    .map(Payment::getAmountPayed)
                    .reduce(BigInteger.ZERO, BigInteger::add);
            BigInteger transferPaymentsSum = payments.stream()
                    .filter(payment -> payment.getWebshopId().equals(webshopId))
                    .filter(payment -> payment.getType().equals("transfer"))
                    .map(Payment::getAmountPayed)
                    .reduce(BigInteger.ZERO, BigInteger::add);
            stringBuilder.append(webshopId).append(";")
                    .append(cardPaymentsSum).append(";")
                    .append(transferPaymentsSum).append(";");
            report.add(stringBuilder.toString());
        }
        return report;
    }
}
