package taxicab.taxicab.model;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("MINI")
public class Mini extends Vehicle {

    public Mini() {}

    @Override
    public String getCategory() {
        return "Mini";
    }
}
