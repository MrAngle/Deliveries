package AvengaInterview.InterviewDemo.Services;

import AvengaInterview.InterviewDemo.model.CarDelivery;
import AvengaInterview.InterviewDemo.model.DeliveryPackage;
import AvengaInterview.InterviewDemo.repository.DeliveryPackageRepository;
import AvengaInterview.InterviewDemo.repository.DeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeliveryPackageService {

    DeliveryPackageRepository deliveryPackageRepository;
    DeliveryRepository deliveryRepository;

    @Autowired
    public DeliveryPackageService(DeliveryPackageRepository deliveryPackageRepository, DeliveryRepository deliveryRepository) {
        this.deliveryPackageRepository = deliveryPackageRepository;
        this.deliveryRepository = deliveryRepository;
    }

    public void saveDeliveryPackage(DeliveryPackage deliveryPackage) {
        deliveryPackageRepository.save(deliveryPackage);
    }

    public void saveDelivery(CarDelivery delivery) {
        deliveryRepository.save(delivery);
    }
}
