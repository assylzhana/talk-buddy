package kz.diploma.talk_buddy.repository;

import kz.diploma.talk_buddy.entity.Level;
import kz.diploma.talk_buddy.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByLevelOrderByIdAsc(Level level);
    long countByLevel(Level level);
}