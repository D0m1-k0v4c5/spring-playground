package com.example.springplayground.account;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author d0m1.k0v4c5 on 2024. 08. 19.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {
}
