package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Administrator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByEmailIgnoreCase(String email);

    List<Administrator> findAllByOrderByCreatedAtAsc();

    boolean existsByEmailIgnoreCase(String email);
}
