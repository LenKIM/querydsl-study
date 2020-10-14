package study.querydsl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "customer")
    @Fetch(FetchMode.SUBSELECT)
    private Set<AOrder> AOrders = new HashSet<>();


    public Set<AOrder> getAOrders() {
        return AOrders;
    }

    public void setAOrders(Set<AOrder> AOrders) {
        this.AOrders = AOrders;
    }
}