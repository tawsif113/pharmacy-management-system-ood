package com.arima.pms.repository;

import com.arima.pms.domain.entity.Role;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
}
