package com.leonard.web_authn.feature.authorization.adapter.repository;

import com.leonard.web_authn.feature.authorization.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);

    List<Role> findByIsActiveTrue();
}
