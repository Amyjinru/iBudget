package com.accounting.repository;

import com.accounting.model.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, String> {
    List<UserToken> findByUserId(String userId);
    Optional<UserToken> findByToken(String token);
    void deleteByToken(String token);
    void deleteByUserId(String userId);
}
