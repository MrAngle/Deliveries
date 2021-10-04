package AvengaInterview.InterviewDemo;

import AvengaInterview.InterviewDemo.Services.DeliveryPackageService;
import AvengaInterview.InterviewDemo.model.CarDelivery;
import AvengaInterview.InterviewDemo.model.DeliveryPackage;
import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

@Service
public class SimulateTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulateTransport.class);

    static final int MAX_PACKAGES_IN_CAR = 250;

    List<DeliveryPackage> deliveryPackages = new ArrayList<>();
    List<CarDelivery> carDeliveries = new ArrayList<>();

    DeliveryPackageService deliveryPackageService;

    @Autowired
    public SimulateTransport(DeliveryPackageService deliveryPackageService) {
        this.deliveryPackageService = deliveryPackageService;
    }


    public void runEnviroment() {
        // simulate
        deliveryPackages = loadNewPackagesList();
        mergePackages();
        checkResults();
        try {
            resultsToCSVFile(carDeliveries);
        } catch (IOException e) {
            // TODO: handle it
            LOGGER.error(e.getMessage());
        }

    }

    void mergePackages() {
        int itemIndex = 0;
        for (DeliveryPackage deliveryPackage: deliveryPackages) {

            if(!deliveryPackage.isHasBeenSent()) {
                deliveryPackage.setHasBeenSent(true);
                int itemsToFillCar = MAX_PACKAGES_IN_CAR - deliveryPackage.getNumberOfItemsInPackage();

                List<DeliveryPackage> deliveryPackagesToCombine = combinePackagesNoRecureny(itemsToFillCar, deliveryPackages);
                deliveryPackagesToCombine.add(deliveryPackage);

                CarDelivery delivery = CarDelivery.builder()
                        .listOfPackages(CarDelivery.getListOfPackagesAsString(deliveryPackagesToCombine))
                        .numberOfItemsInPackage(CarDelivery.getNumberOfItemsInPackage((deliveryPackagesToCombine)))
                        .deliveryPackages(deliveryPackagesToCombine)
                        .build();

                deliveryPackageService.saveDelivery(delivery);
                carDeliveries.add(delivery);
                // LOGGER.info("SET DELIVERY FOR: " + itemIndex + " " + delivery);
            }
            itemIndex = itemIndex + 1;
        }

        for (DeliveryPackage deliveryPackage: deliveryPackages) {
            deliveryPackageService.saveDeliveryPackage(deliveryPackage);
        }
    }

    boolean smallerPackageExists(List<DeliveryPackage> deliveryPackagesToCheck, int numberOfItemsToFind) {
        List<DeliveryPackage> avaiablePackagesToMerge =
                deliveryPackagesToCheck.stream()
                        .filter(dP -> dP.getNumberOfItemsInPackage() <= numberOfItemsToFind && !dP.isHasBeenSent())
                        .sorted((r1, r2) -> r2.getNumberOfItemsInPackage() - r1.getNumberOfItemsInPackage()).toList();

        return !avaiablePackagesToMerge.isEmpty();
    }


    List<DeliveryPackage> getAvailableDeliveryPackages(List<DeliveryPackage> packages, int finalNumberOfItemsToFind) {
        return packages.stream()
                .filter(dP -> dP.getNumberOfItemsInPackage() <= finalNumberOfItemsToFind && !dP.isHasBeenSent())
                .sorted((r1, r2) -> r2.getNumberOfItemsInPackage() - r1.getNumberOfItemsInPackage()).toList();
    }

    Optional<DeliveryPackage> getBestPackageToCombine(List<DeliveryPackage> packages, int numberOfItemsToFind) {
        for (DeliveryPackage deliveryPackage: packages) {
            deliveryPackage.setHasBeenSent(true);
            if(smallerPackageExists(packages,
                    numberOfItemsToFind - deliveryPackage.getNumberOfItemsInPackage())) {
                return Optional.of(deliveryPackage);
            } else {
                deliveryPackage.setHasBeenSent(false);
            }
        }
        return Optional.empty();
    }

    List<DeliveryPackage> combinePackagesNoRecureny(int itemsTofind, List<DeliveryPackage> packages) {
        int currentNumberOfItemsToFind;
        List<DeliveryPackage> resultsDeliveryPackage = new ArrayList<>();

        while(true) {
            int sumOfItems = CarDelivery.getNumberOfItemsInPackage(resultsDeliveryPackage);
            currentNumberOfItemsToFind = itemsTofind - sumOfItems;

            List<DeliveryPackage> availablePackagesToMerge = getAvailableDeliveryPackages(packages,
                    currentNumberOfItemsToFind);
            DeliveryPackage deliveryPackageToAdd;

            // Skip if no available packages
            if(availablePackagesToMerge.isEmpty()) {
                return resultsDeliveryPackage;
            }
            DeliveryPackage largesPackage = availablePackagesToMerge.get(0);

            // Perfect scenario
            if(largesPackage.getNumberOfItemsInPackage() == currentNumberOfItemsToFind) {
                largesPackage.setHasBeenSent(true);
                resultsDeliveryPackage.add(largesPackage);
                return resultsDeliveryPackage;
            }

            // Check if adding a package allows to combine another available package to get best results
            Optional<DeliveryPackage> optBestPackageToCombine = getBestPackageToCombine(availablePackagesToMerge,
                    currentNumberOfItemsToFind);

            if(optBestPackageToCombine.isPresent()) {
                deliveryPackageToAdd = optBestPackageToCombine.get();
            } else {
                largesPackage.setHasBeenSent(true);
                deliveryPackageToAdd = largesPackage;
            }
            resultsDeliveryPackage.add(deliveryPackageToAdd);
        }
    }

    void checkResults() {
        boolean failed = false;

        int allLaptops = CarDelivery.getNumberOfItemsInPackage(deliveryPackages);
        LOGGER.info("[PERFECT SCENARIO] ALL LAPTOPS: {}", allLaptops);
        LOGGER.info("[PERFECT SCENARIO] Min Deliveries: {}", allLaptops/(MAX_PACKAGES_IN_CAR * 1.0) );

        LOGGER.info("[ACTUAL SCENARIO] number of deliveries: {}", carDeliveries.size());
        LOGGER.info("[ACTUAL SCENARIO] number of deliveries: {}",
                CarDelivery.getNumberOfItemsInPackage(carDeliveries.stream().map(CarDelivery::getDeliveryPackages)
                        .flatMap(Collection::stream).toList()));

        // Better option will be use some kind of "delta".
        if(carDeliveries.size() != Math.ceil(allLaptops/(MAX_PACKAGES_IN_CAR * 1.0))) {
            LOGGER.info("Not optimized correctly");
            failed = true;
        }


        LOGGER.info("Check packages...");
        // Create list of packages by sum all packages from deliveries
        List<DeliveryPackage> deliveryPackageToCheck  = carDeliveries.stream()
                .map(CarDelivery::getDeliveryPackages)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(DeliveryPackage::getName))
                .sorted((d1, d2) -> d2.getNumberOfItemsInPackage() - d1.getNumberOfItemsInPackage())
                .toList();

        deliveryPackages = deliveryPackages.stream()
                .sorted(Comparator.comparing(DeliveryPackage::getName))
                .sorted((d1, d2) -> d2.getNumberOfItemsInPackage() - d1.getNumberOfItemsInPackage())
                .toList();

        for (int i = 0; i < deliveryPackages.size(); i++) {
            if(!deliveryPackages.get(i).equals(deliveryPackageToCheck.get(i))) {
                LOGGER.info("{} is not equal to: {}", deliveryPackageToCheck.get(i), deliveryPackages.get(i));
                failed = true;
            }
        }

        // all should "hasBeenSent"
        for (DeliveryPackage deliveryPackage : deliveryPackageToCheck) {
            if(!deliveryPackage.isHasBeenSent()) {
                LOGGER.error("{} has not been sent!", deliveryPackage);
                failed = true;
            }
        }

        if(failed) {
            LOGGER.info("FAILED");
        } else {
            LOGGER.info("SUCCESS");
        }

    }

    List<DeliveryPackage> loadNewPackagesList(){
        List<DeliveryPackage> records = null;
        try {
            records = readCSVFile();
            records.sort((r1, r2) -> r2.getNumberOfItemsInPackage() - r1.getNumberOfItemsInPackage());
            for (DeliveryPackage deliveryPackage: records) {
                deliveryPackageService.saveDeliveryPackage(deliveryPackage);
            }
        } catch (URISyntaxException e) {
            // TODO: handle it
        }
        return records;
    }

    List<DeliveryPackage> readCSVFile() throws URISyntaxException {

        List<DeliveryPackage> records = new ArrayList<>();
        File file;
        try {
            URL res = getClass().getClassLoader().getResource("Data.csv");
            file = Paths.get(res.toURI()).toFile();
        } catch (URISyntaxException e) {
            // TODO: handle it
            e.printStackTrace();
            return null;
        }

        CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
        try (CSVReader csvReader = new CSVReaderBuilder(new FileReader(file)).withCSVParser(parser).withSkipLines(1).build()) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                DeliveryPackage deliveryPackage = DeliveryPackage.builder()
                        .name(values[0]).numberOfItemsInPackage(Integer.parseInt(values[1])).build();
                        // Algorithm can be tested by using random values
//                         .name(values[0]).numberOfItemsInPackage(ThreadLocalRandom.current().nextInt(1, 249)).build();

                records.add(deliveryPackage);
            }
        } catch (CsvValidationException | IOException e) {
            // TODO: handle it
            e.printStackTrace();
        }
        return records;
    }

    void resultsToCSVFile(List<CarDelivery> carDeliveries) throws IOException {
        String pathToFile = Objects.requireNonNull(this.getClass().getResource("/")).getPath();

        ICSVWriter writer = new CSVWriterBuilder(new FileWriter(pathToFile + "/DataOutput.csv"))
                .withSeparator(';').build();
        String[] header = { "ID", "PACKAGE_NAMES", "Marks" };
        writer.writeNext(header);
        for (CarDelivery carDeliver : carDeliveries) {
            String[] array = new String[] {
                    String.valueOf(carDeliver.getId()),
                    CarDelivery.getListOfPackagesAsString(carDeliver.getDeliveryPackages()),
                    String.valueOf(CarDelivery.getNumberOfItemsInPackage(carDeliver.getDeliveryPackages())),
            };
            writer.writeNext(array);
        }
        writer.close();
        LOGGER.info(String.format("File has been saved: %s", pathToFile));
    }
}