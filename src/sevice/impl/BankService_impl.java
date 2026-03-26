package sevice.impl;

import domain.Account;
import domain.Customer;
import domain.Transaction;
import domain.Type;
import exceptions.AccountNotFoundException;
import exceptions.InsufficientBalanceException;
import exceptions.ValidationException;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import sevice.BankService;
import utils.Validation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BankService_impl implements BankService {
   private final AccountRepository accountRepo=new AccountRepository();
   private final TransactionRepository transactionRepository=new TransactionRepository();
   private final CustomerRepository customerRepository=new CustomerRepository();

//   private final Validation<String> validateName=name->{
//       if(name==null){
//           throw new ValidationException("Name is required");
//       }
//   };
//    private final Validation<String> validateEmail=email->{
//        if(email==null || !email.contains("@")){
//            throw new ValidationException("Email is required");
//        }
//    };
//    private final Validation<String> validateType=type->{
//        if(!type.equalsIgnoreCase("SAVING") && !type.equalsIgnoreCase("CURRENT")){
//            throw new ValidationException("AccountType Must be SAVING or CURRENT");
//        }
//    };

    private final Validation<String> validateName=name->{
        if(name==null){
            System.out.println("Name is required!");
        }
    };
    private final Validation<String> validateEmail=email->{
        if(email==null || !email.contains("@")){
            throw new ValidationException("Email is required");
        }
    };
    private final Validation<String> validateType=type->{
        if(!type.equalsIgnoreCase("SAVING") && !type.equalsIgnoreCase("CURRENT")){
            throw new ValidationException("AccountType Must be SAVING or CURRENT");
        }
    };

    @Override
    public void openAccount(String name, String email, String accountType , double bal) {
        validateName.validate(name);
        validateEmail.validate(email);
        validateType.validate(accountType);
       String customerId =UUID.randomUUID().toString();
       Customer c=new Customer(customerId,name,email);
       customerRepository.save(c);
       // AC000001 --> AC000001+1 .....
       //String accountNumber=UUID.randomUUID().toString();
        String accountNumber = getAccountNumber();
        Account account=new Account(accountNumber,customerId,bal,accountType);
        accountRepo.save(account);
        System.out.println("\nAccount opened , with initial balance : "+bal+"  Ac.No. : "+accountNumber+"\n");
    }

    @Override
    public List<Account> listAccounts() {
        return accountRepo.findAll().stream()
               .sorted(Comparator.comparing(Account::getAccountNumber))
                .collect(Collectors.toList());
    }

    @Override
    public void deposit(String accountNumber, Double amount, String note) {
        Account account=accountRepo.findByNumber(accountNumber)
                .orElseThrow(()->new AccountNotFoundException("Account Not Found: "+ accountNumber));
        account.setBalance(account.getBalance()+amount);
        Transaction transaction=new Transaction(UUID.randomUUID().toString(), Type.DEPOSIT,account.getAccountNumber(),amount, LocalDateTime.now(),note);
        transactionRepository.add(transaction);
    }

    @Override
    public void withdraw(String accountNumber, Double amount, String note) {
        Account account=accountRepo.findByNumber(accountNumber)
                .orElseThrow(()->new AccountNotFoundException("Account Not Found: "+ accountNumber));
        if(account.getBalance().compareTo(amount)<0)
            throw new InsufficientBalanceException("Insufficient Balance.");
        account.setBalance(account.getBalance()-amount);
        Transaction transaction=new Transaction(UUID.randomUUID().toString(), Type.WITHDRAW,account.getAccountNumber(),amount, LocalDateTime.now(),note);
        transactionRepository.add(transaction);
    }

    @Override
    public void transfer(String fromAcc, String toAcc, Double amount, String note) {
        if(fromAcc.equals(toAcc))
            throw new ValidationException("Cannot transfer to your own Account!");
        Account from =accountRepo.findByNumber(fromAcc)
                .orElseThrow(()->new AccountNotFoundException("Account Not Found!"));
        Account to =accountRepo.findByNumber(toAcc)
                .orElseThrow(()->new AccountNotFoundException("Account Not Found!"));

        if(from.getBalance().compareTo(amount)<0)
            throw new InsufficientBalanceException("Insufficient Balance!");

        from.setBalance(from.getBalance()-amount);
        to.setBalance(to.getBalance()+amount);

        transactionRepository.add(new Transaction(UUID.randomUUID().toString(),
                Type.TRANSFER_IN,from.getAccountNumber(),amount,
                LocalDateTime.now(),note));

        transactionRepository.add(new Transaction(UUID.randomUUID().toString(),
                Type.TRANSFER_OUT,from.getAccountNumber(),amount,
                LocalDateTime.now(),note));

    }

    @Override
    public List<Transaction> getStatement(String account) {
        return transactionRepository.findByAccount(account).stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp))
                .collect(Collectors.toList());
    }

    @Override
    public List<Account> searchAccountByCustomerName(String q) {
        String query=(q==null) ? "" : q.toLowerCase();
        List<Account> result=new ArrayList<>();
        for(Customer c: customerRepository.findAll()){
            if(c.getName().toLowerCase().contains(query)){
                result.addAll(accountRepo.findByCustomerId(c.getId()));
            }
        }
        result.sort(Comparator.comparing(Account::getAccountNumber));
        return result;
    }

    private String getAccountNumber() {
        int temp=accountRepo.findAll().size()+1;
        return String.format("AC%06d",temp);
    }
}
