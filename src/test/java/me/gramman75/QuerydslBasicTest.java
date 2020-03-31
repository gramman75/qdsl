package me.gramman75;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import me.gramman75.domain.Member;
import me.gramman75.domain.QMember;
import me.gramman75.domain.QTeam;
import me.gramman75.domain.Team;
//import me.gramman75.repogitory.MemberRepository;
//import me.gramman75.repogitory.QueryRepository;
//import me.gramman75.repogitory.TeamRepository;
import me.gramman75.dto.MemberDto;
import me.gramman75.dto.QMemberDto;
import me.gramman75.dto.UserDto;
import org.aspectj.lang.annotation.Before;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.annotation.Persistent;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static me.gramman75.domain.QMember.*;
import static me.gramman75.domain.QTeam.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

//    @Autowired
//    MemberRepository memberRepository;
//
//    @Autowired
//    TeamRepository teamRepository;
//
//    @Autowired
//    QueryRepository queryRepository;

    @PersistenceContext
    EntityManager em;

    private JPAQueryFactory query;

    @BeforeEach
    public void init(){
        query = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

//        teamRepository.save(teamA);
//        teamRepository.save(teamB);
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

//        memberRepository.saveAll(new ArrayList<Member>(
//                Arrays.asList(member1, member2, member3, member4)
//        ));
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

    }

//    @Test
//    public void startQuerydsl(){
//        Member member = queryRepository.findByName("member1");
//        assertThat(member.getUsername()).isEqualTo("member1");
//    }

    @Test
    public void 조건() {
        List<Member> members = query
                .select(QMember.member)
                .from(QMember.member)
                .where(QMember.member.username.contains("member"),
                        QMember.member.age.gt(20))
                .fetch();

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    public void 결과조회() {
        List<Member> fetch = query
                .selectFrom(member)
                .fetch();

        assertThat(fetch.size()).isEqualTo(4);

        Member fetchOne = query
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(fetchOne.getUsername()).isEqualTo("member1");

        Member fetchFirst = query
                .selectFrom(QMember.member)
                .orderBy(QMember.member.username.desc())
                .fetchFirst();

        assertThat(fetchFirst.getUsername()).isEqualTo("member4");

        QueryResults<Member> fetchResults = query
                .selectFrom(member)
                .where(member.age.goe(20))
                .offset(2)
                .limit(1)
                .fetchResults();

        assertThat(fetchResults.getTotal()).isEqualTo(3);
        System.out.println(fetchResults.getResults().size());
        assertThat(fetchResults.getResults().size()).isEqualTo(1);
    }

    @Test
    public void 정렬(){
//        memberRepository.saveAll( new ArrayList<>
//            (Arrays.asList(
//                    new Member(null, 100),
//                    new Member("member5", 100),
//                    new Member("member6", 100)
//            )
//        ));

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members =  query
                .select(member)
                .from(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();

    }

    @Test
    public void 집합함수() {
        Tuple tuple = query
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetchOne();

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void group(){
        List<Tuple> result = query
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        result.forEach(tuple -> System.out.println(tuple));

        Tuple teamA = result.stream()
                .filter(tuple -> tuple.get(team.name).equals("teamA"))
                .findAny()
                .orElse(null);

        Tuple teamB = result.stream()
                .filter(tuple -> tuple.get(team.name).equals("teamB"))
                .findAny()
                .orElse(null);

        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    public void 기본조인() {
        List<Member> result = query
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @Test
    public void 세타조인() {
//        memberRepository.save(new Member("teamA"));
//        memberRepository.save(new Member("teamB"));
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = query
                .select(member, team)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        result.forEach(System.out::println);

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void On조인() {
        List<Tuple> result = query
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();


        result.forEach(System.out::println);

        assertThat(result.size()).isEqualTo(4);
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void 페치조인() {
        em.flush();
        em.clear();

        Member result = query
                .select(member)
                .from(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());

        assertThat(loaded).isTrue();


    }

    @Test
    public void 서브쿼리from절() {
        QMember submember = new QMember("submember");
        List<Member> result = query
                .selectFrom(member)
                .where(member.age.eq(
                        select(submember.age.max())
                                .from(submember)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @Test
    public void 서브쿼리Select절() {
        QMember submember = new QMember("submember");

        List<Tuple> result = query
                .select(member.username,
                        select(submember.age.avg())
                                .from(submember)
                ).from(member)
                .fetch();


        result.forEach(System.out::println);

    }

    @Test
    public void 단순한Case문() {
        List<Tuple> result = query
                .select(member.username,
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타"))
                .from(member)
                .fetch();

        assertThat(result).extracting(tuple -> tuple.get(1, String.class))
                .containsExactlyInAnyOrder("스무살", "기타", "기타", "열살");
    }

    @Test
    public void 복잡한Case문() {
        List<Tuple> result = query
                .select(member.username, new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void ProjectionSetter() {
       
        List<MemberDto> result = query
                .select(
                        Projections.bean(
                                MemberDto.class,
                                member.username,
                                member.age,
                                team.name.as("teamname"))
                )
                .from(member)
                .leftJoin(member.team, team).fetchJoin()
                .fetch();

        result.forEach(System.out::println);
    }



    @Test
    public void ProjectionField() {
        List<MemberDto> result = query
                .select(
                        Projections.fields(
                                MemberDto.class,
                                member.username,
                                member.age)
                )
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }


    @Test
    public void ProjectionConstructor() {
        List<MemberDto> result = query
                .select(
                        Projections.constructor(
                                MemberDto.class,
                                member.username,
                                member.age)
                )
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void ProjectionAlias() {
        List<UserDto> result = query
                .select(
                        Projections.fields(
                                UserDto.class,
                                member.username.as("name"),
                                member.age,
                                ExpressionUtils.as(
                                        select(team.name)
                                                .from(team)
                                                .where(member.team.id.eq(team.id)),
                                        "teamname"
                                )
                        )
                )
                .from(member)
                .fetch();

        result.forEach(System.out::println);
    }

    @Test
    public void QueryProjection(){
        List<MemberDto> result = query
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        result.forEach(System.out::println);
    }







}
