package com.accounting.repository;

import com.accounting.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, String> {
    List<Budget> findByUserId(String userId);
    
    // Find budget by user, year, month
    List<Budget> findByUserIdAndYearAndMonth(String userId, int year, int month);
    
    // Find specific category budget
    Optional<Budget> findByUserIdAndCategoryIdAndYearAndMonth(String userId, String categoryId, int year, int month);
    
    // Find total budget (categoryId is null)
    Optional<Budget> findByUserIdAndCategoryIdIsNullAndYearAndMonth(String userId, int year, int month);
}
