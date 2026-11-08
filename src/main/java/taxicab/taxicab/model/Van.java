package taxicab.taxicab.model;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("VAN")
public class Van extends Vehicle {

    public Van() {}

    @Override
    public String getCategory() {
        return "Van";
    }
}
