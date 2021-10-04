package AvengaInterview.InterviewDemo.repository;

import AvengaInterview.InterviewDemo.model.CarDelivery;
import org.springframework.data.repository.CrudRepository;

public interface DeliveryRepository extends CrudRepository<CarDelivery, Long> {
}
