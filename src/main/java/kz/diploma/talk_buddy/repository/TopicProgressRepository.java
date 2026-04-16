package kz.diploma.talk_buddy.repository;

import kz.diploma.talk_buddy.entity.Topic;
import kz.diploma.talk_buddy.entity.TopicProgress;
import kz.diploma.talk_buddy.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicProgressRepository extends JpaRepository<TopicProgress, Long> {
    @Query("""
    SELECT COUNT(DISTINCT tp.user.id)
    FROM TopicProgress tp
    WHERE tp.topic = :topic
""")
    long countStudentsByTopic(@Param("topic") Topic topic);
    List<TopicProgress> findByTopic(Topic topic);
    TopicProgress findTopByUserAndTopicOrderByIdDesc(User user, Topic topic);
}
