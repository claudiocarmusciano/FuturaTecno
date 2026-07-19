package com.futuratecno.infrastructure;

import com.futuratecno.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    Optional<Usuario> findByGoogleSub(String googleSub);
    Optional<Usuario> findByResetToken(String resetToken);
    boolean existsByEmailIgnoreCase(String email);
    List<Usuario> findByRolOrderByCreatedAtDesc(String rol);
    long countByRol(String rol);
}
