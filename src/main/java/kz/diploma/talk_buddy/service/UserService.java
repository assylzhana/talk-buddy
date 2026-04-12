package kz.diploma.talk_buddy.service;

import kz.diploma.talk_buddy.entity.Role;
import kz.diploma.talk_buddy.entity.User;
import kz.diploma.talk_buddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findStudents(String keyword) {

        Sort sort = Sort.by(Sort.Direction.ASC, "id");

        if (keyword == null || keyword.isBlank()) {
            return userRepository.findByRole(Role.ROLE_STUDENT, sort);
        }

        return userRepository.findByUsernameContainingIgnoreCaseAndRole(
                keyword,
                Role.ROLE_STUDENT,
                sort
        );
    }
}