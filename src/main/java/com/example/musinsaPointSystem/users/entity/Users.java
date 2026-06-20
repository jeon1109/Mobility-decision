package com.example.musinsaPointSystem.users.entity;

import com.example.musinsaPointSystem.common.BaseEntity;
import com.example.musinsaPointSystem.users.enm.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Entity
@Builder
@Table(name = "users_login")
@ToString(of = {"uid", "name", "email"})
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA에서는 protected 레벨의 기본 생성자 필요
@AllArgsConstructor
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long uid;

    @NotNull(message = "이름은 필수입니다.")
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull(message = "이메일은 필수입니다.")
    @Column(name = "email", nullable = false)
    private String email;

    @NotNull(message = "비밀번호는 필수입니다.")
    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "del_yn", nullable = false)
    private boolean delYn;

    // 스스로 상태 변경으로 사용하는 거
}
