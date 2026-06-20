package com.example.musinsaPointSystem.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.musinsaPointSystem.users.entity.Users;

public interface UserRepository extends JpaRepository<Users, Long> {
	Optional<Users> findByEmail(String email);

	Optional<Users> findByEmailAndPassword(String email, String password);

	boolean existsByEmail(String username);
}
