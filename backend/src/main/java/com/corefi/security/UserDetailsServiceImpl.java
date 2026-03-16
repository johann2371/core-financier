package com.corefi.security;

import com.corefi.entity.Utilisateur;
import com.corefi.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        return new User(
                utilisateur.getEmail(),
                utilisateur.getPassword(),
                utilisateur.isActif(),
                true,
                true,
                utilisateur.getTentativesConnexion() < 5,
                Collections.singletonList(new SimpleGrantedAuthority(utilisateur.getRole().name())));
    }
}
