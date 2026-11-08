package taxicab.taxicab.model;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("CAR")
public class Car extends Vehicle {

    public Car() {}

    @Override
    public String getCategory() {
        return "Car";
    }
}
