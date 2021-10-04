package AvengaInterview.InterviewDemo.model;

import lombok.*;

import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CarDelivery {

    // Should be as class field - not static value
    static final int CAPACITY = 250;

    @Id
    @GeneratedValue
    private long id;

    // should be removed - used for testing
    @Column(nullable = false, length = 2000)
    private String listOfPackages;

    // should be removed - used for testing
    @Column(nullable = false)
    private int numberOfItemsInPackage;

    @OneToMany
    private List<DeliveryPackage> deliveryPackages;

    public static CarDeliveryBuilder builder() {
        return new CustomBuilder();
    }

    private static class CustomBuilder extends CarDeliveryBuilder {
        public CarDelivery build() {
            if (getNumberOfItemsInPackage(super.deliveryPackages) > CAPACITY) {
                throw new RuntimeException("Invaid number of items in car");
            }
            return super.build();
        }
    }

    public static String getListOfPackagesAsString(List<DeliveryPackage> deliveryPackages) {
        return deliveryPackages.stream()
                .map(DeliveryPackage::getName).collect(Collectors.joining("','", "[", "]"));
    }

    public static int getNumberOfItemsInPackage(List<DeliveryPackage> deliveryPackages) {
        int summary = 0;
        if(deliveryPackages != null) {
            for (DeliveryPackage deliveryPackage: deliveryPackages) {
                summary = summary + deliveryPackage.getNumberOfItemsInPackage();
            }
        }
        return summary;
    }
}
