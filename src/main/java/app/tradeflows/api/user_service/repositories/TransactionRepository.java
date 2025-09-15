package app.tradeflows.api.user_service.repositories;

import app.tradeflows.api.user_service.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
}
