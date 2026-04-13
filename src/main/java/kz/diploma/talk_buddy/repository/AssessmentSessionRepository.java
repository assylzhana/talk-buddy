package kz.diploma.talk_buddy.repository;

import kz.diploma.talk_buddy.entity.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {

    Optional<AssessmentSession> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}