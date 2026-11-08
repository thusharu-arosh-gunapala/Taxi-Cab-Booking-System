package taxicab.taxicab.model;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("PREMIUM")
public class Premium extends Vehicle {

    public Premium() {}

    @Override
    public String getCategory() {
        return "Premium";
    }
}
