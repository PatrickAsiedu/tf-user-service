package app.tradeflows.api.user_service.repositories;

import app.tradeflows.api.user_service.entities.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByUser_Email(String email);
    Optional<Account> findByUser_Id(String id);
}
