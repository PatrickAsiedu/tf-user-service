package app.tradeflows.api.user_service.entities;

import app.tradeflows.api.user_service.enums.TransactionStatus;
import app.tradeflows.api.user_service.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;


@Table(name = "tf_transactions")
@Entity
@Setter
@Getter
@NoArgsConstructor
public class Transaction extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, length = 200)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    @JsonIgnore
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private double amount;
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;


    public Transaction(Account account, User user, double amount, String description, TransactionType transactionType, TransactionStatus status) {
        this.account = account;
        this.user = user;
        this.amount = amount;
        this.description = description;
        this.transactionType = transactionType;
        this.status = status;
    }
}
