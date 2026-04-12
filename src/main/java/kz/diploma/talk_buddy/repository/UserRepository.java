package kz.diploma.talk_buddy.repository;

import kz.diploma.talk_buddy.entity.Role;
import kz.diploma.talk_buddy.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByUsernameContainingIgnoreCaseAndRole(String username, kz.diploma.talk_buddy.entity.Role role, Sort sort);
    List<User> findByRole(Role role, Sort sort);
}