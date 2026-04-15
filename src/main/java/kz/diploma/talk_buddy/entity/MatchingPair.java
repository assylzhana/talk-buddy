package kz.diploma.talk_buddy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "matching_pairs")
@Getter
@Setter
public class MatchingPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String leftText;
    private String rightText;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
}