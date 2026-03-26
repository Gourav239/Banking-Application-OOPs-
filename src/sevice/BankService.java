package sevice;

import domain.Account;
import domain.Transaction;

import java.util.List;

public interface BankService {
    void openAccount(String name, String email, String accountType , double bal);
    List<Account> listAccounts();

    void deposit(String accountNumber, Double amount, String deposit);

    void withdraw(String accountNumber, Double amount, String withdrawal);

    void transfer(String from, String to, Double amount, String transfer);

    List<Transaction> getStatement(String account);

    List<Account> searchAccountByCustomerName(String q);
}


