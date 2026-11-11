package com.serendib.taxibooking.repository;

import com.serendib.taxibooking.entity.DriverPasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface DriverPasswordResetTokenRepository extends JpaRepository<DriverPasswordResetToken, Long> {

    @Query("""
            select token from DriverPasswordResetToken token
            where token.tokenHash = :tokenHash and token.expiresAt > :now
            """)
    Optional<DriverPasswordResetToken> findValidToken(
            @Param("tokenHash") String tokenHash, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token from DriverPasswordResetToken token
            join fetch token.driver
            where token.tokenHash = :tokenHash and token.expiresAt > :now
            """)
    Optional<DriverPasswordResetToken> findValidTokenForUpdate(
            @Param("tokenHash") String tokenHash, @Param("now") Instant now);

    void deleteByDriverId(Long driverId);
}
