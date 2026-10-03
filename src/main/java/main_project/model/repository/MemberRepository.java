package main_project.model.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import main_project.model.entity.MemberEntity;


@Repository 
public interface MemberRepository extends JpaRepository <MemberEntity , String> {


// 로그인용 회원 조회
Optional<MemberEntity> findByUserEmail(String userEmail);

//  회원 전체 조회 + 역할(role)까지 한 번에 조회
//  roleEntity 는 LAZY 라서 그냥 findAll() 하면 getRoleEntity() 할 때마다 role 쿼리가 추가로 나감.
//  JOIN FETCH 로 처음부터 같이 가져오면 쿼리 1번으로 끝.
    @Query("SELECT m FROM MemberEntity m JOIN FETCH m.roleEntity")
    List<MemberEntity> findAllWithRole();
}
