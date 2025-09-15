package app.tradeflows.api.user_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Table(name = "tf_accounts")
@Entity
@Setter
@Getter
public class Account extends Audit{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, length = 200)
    private String id;
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User user;
    private double availableBalance;
    private double lockedAmount;
    private boolean isActive;

    public Account(){}

    public Account(User user, double availableBalance, double lockedAmount) {
        this.user = user;
        this.availableBalance = availableBalance;
        this.lockedAmount = lockedAmount;
    }

    public Account(User user, double availableBalance, double lockedAmount, boolean isActive) {
        this(user, availableBalance, lockedAmount);
        this.isActive = isActive;
    }
}
