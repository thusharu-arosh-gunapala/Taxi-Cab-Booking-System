package taxicab.taxicab.model;

import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

@Entity
@DiscriminatorValue("TUK_TUK")
public class TukTuk extends Vehicle {

    public TukTuk() {}

    @Override
    public String getCategory() {
        return "Tuk Tuk";
    }
}
