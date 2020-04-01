package me.gramman75.repository;

import me.gramman75.domain.Member;
import me.gramman75.domain.Team;
import me.gramman75.dto.MemberSearchCond;
import me.gramman75.dto.MemberTeamDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() {
        Member member = new Member("member1", 10);

        memberJpaRepository.save(member);

        Optional<Member> findMember1 = memberJpaRepository.findByid(member.getId());
        assertThat(findMember1.map(Member::getUsername).orElse("unknown")).isEqualTo("member1");

        List<Member> allMember = memberJpaRepository.findByAll();
        assertThat(allMember).extracting("username").contains("member1");

        List<Member> member1 = memberJpaRepository.findByUsername("member1");
        assertThat(member1).extracting("username").contains("member1");


        List<Member> allMemberQuerydsl = memberJpaRepository.findByAllQuerydsl();
        assertThat(allMemberQuerydsl).extracting("username").contains("member1");

        List<Member> member1Querydsl = memberJpaRepository.findByUsernameQuerydsl("member1");
        assertThat(member1Querydsl).extracting("username").contains("member1");
    }

    @Test
    public void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCond cond = new MemberSearchCond();
        cond.setAgeGoe(35);
        cond.setAgeLoe(40);
        cond.setTeamname("teamB");

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(cond);

        assertThat(result).extracting("username").contains("member4");
    }
}