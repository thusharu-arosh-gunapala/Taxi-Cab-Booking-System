package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.Promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.code = :code")
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);
}
